/*
 * Copyright (c) 2013-2014, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2014, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.script

import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.exception.ProcessScriptException
import nextflow.executor.AbstractExecutor
import nextflow.executor.LocalExecutor
import nextflow.executor.LsfExecutor
import nextflow.executor.NopeExecutor
import nextflow.executor.SgeExecutor
import nextflow.executor.SlurmExecutor
import nextflow.executor.SupportedScriptTypes
import nextflow.processor.MergeTaskProcessor
import nextflow.processor.ParallelTaskProcessor
import nextflow.processor.TaskConfig
import nextflow.processor.TaskConfigWrapper
import nextflow.processor.TaskProcessor
import nextflow.util.CacheHelper
import nextflow.util.FileHelper
/**
 * Any user defined script will extends this class, it provides the base execution context
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
abstract class BaseScript extends Script {


    protected BaseScript(){ }

    protected BaseScript(Binding binding) {
        super(binding)
    }

    /*
     * The script execution session, declare it private to prevent the user script to be able to access it
     */
    @Lazy
    private Session session = { getBinding()?.getVariable('__$session') as Session } ()

    @Lazy
    private boolean isTest = {
        getBinding()?.hasVariable('__$TEST') ? getBinding().getVariable('__$TEST') : false
    } ()

    private final random = new Random()

    /**
     * Holds the configuration object which will used to execution the user tasks
     */
    @Lazy
    Map config = { session.config } ()

    @Lazy
    InputStream stdin = { System.in }()

    private TaskProcessor taskProcessor

    /** Access to the last *process* object -- only for testing purpose */
    @PackageScope
    TaskProcessor getTaskProcessor() { taskProcessor }

    private result

    /** Access to the last *process* result -- only for testing purpose */
    @PackageScope
    Object getResult() { result }


    /**
     * Enable disable task 'echo' configuration property
     * @param value
     */
    def void echo(boolean value = true) {
        config.process.echo = value
    }

    /**
     * Stop the current execution returning an error code and message
     *
     * @param exitCode The exit code to be returned
     * @param message The message that will be reported in the log file (optional)
     */
    def void exit(int exitCode, String message = null) {
        if ( exitCode && message ) {
            log.error message
        }
        else if ( message ) {
            log.info message
        }
        System.exit(exitCode)
    }

    /**
     * Stop the current execution returning a 0 error code and the specified message
     *
     * @param message The message that will be reported in the log file
     */
    def void exit( String message ) {
        exit(0, message)
    }

    def void error( String message = null ) {
        throw message ? new ProcessScriptException(message) : new ProcessScriptException()
    }

    /**
     * Create a folder for the given key. It guarantees to return the same folder name
     * the same provided object key.
     *
     * @param key An object to be used as cache-key creating the folder, it can be any object
     *          or an array or objects to use multi-objects key
     *
     * @return The {@code Path} to the cached directory or a newly created folder for the specified key
     */
    Path cacheableDir( Object key ) {
        assert key, "Please specify the 'key' argument on 'cacheableDir' method"

        def hash = CacheHelper.hasher([ session.uniqueId, key, session.cacheable ? 0 : random.nextInt() ]).hash()

        def file = FileHelper.getWorkFolder(session.workDir, hash)
        if( !file.exists() && !file.mkdirs() ) {
            throw new IOException("Unable to create folder: $file -- Check file system permission" )
        }

        return file
    }

    /**
     * Create a file for the given key. It guarantees to return the same file name
     * the same provided object key.
     *
     * @param key
     * @param name
     * @return
     */
    Path cacheableFile( Object key, String name = null ) {

        // the cacheability is guaranteed by the folder
        def folder = cacheableDir(key)

        if( !name ) {
            if( key instanceof File ) {
                name =  key.getName()
            }
            else if( key instanceof Path ) {
                name =  key.getName()
            }
            else {
                name = key.toString()
            }
        }

        return folder.resolve(name)
    }

    /**
     * @return Create a temporary directory
     */
    Path tempDir( String name = null, boolean create = true ) {
        def path = FileHelper.createTempFolder(session.workDir)
        if( name )
            path = path.resolve(name)

        if( !path.exists() && create && !path.mkdirs() )
            throw new IOException("Unable to create folder: $path -- Check file system permission" )

        return path
    }

    /**
     * @return Create a temporary file
     */
    Path tempFile( String name = null, boolean create = false ) {

        if(!name)
            name = 'file.tmp'

        def folder = tempDir()
        def result = folder.resolve(name)
        if( create )
            Files.createFile(result)

        return result
    }

    /**
     * Create a task processor
     *
     * @param name The name of the process as defined in the script
     * @param body The process declarations provided by the user
     * @return The {@code Processor} instance
     */
    private TaskProcessor createProcessor( String name, Closure body, Map options = null ) {
        assert body

        /*
         * check if exists 'attributes' defined in the 'process' scope for this process, e.g.
         *
         * process.$name.attribute1 = xxx
         * process.$name.attribute2 = yyy
         *
         */
        Map importantAttributes = null
        if( config.process instanceof Map && config.process['$'+name] instanceof Map ) {
            importantAttributes = (Map)config.process['$'+name]
        }

        // -- the config object
        def taskConfig = new TaskConfig(this, importantAttributes)

        // -- set 'default' properties defined in the configuration file in the 'process' section
        if( config.process instanceof Map ) {
            config.process .each { String key, value ->
                if( key.startsWith('$')) return
                taskConfig.setProperty(key,value)
            }
        }

        /*
         * options that may be passed along the process declaration e.g.
         *
         * process name ( x: 1, ... ) {
         *
         * }
         */

        options?.each { String key, value -> taskConfig.setProperty(key,value)}

        // -- set the task name in the config object
        if( name ) {
            taskConfig.name = name
        }

        // Invoke the code block, which will return the script closure to the executed
        // As side effect will set all the properties declaration in the 'taskConfig' object
        // Note: the config object is wrapped by a TaskConfigWrapper because it is required
        // to raise a MissingPropertyException when some values is missing, so that the Closure
        // will try to fallback on the owner object
        def script = new TaskConfigWrapper(taskConfig).with ( body ) as TaskBody
        if ( !script )
            throw new IllegalArgumentException("Missing script in the specified process block -- make sure it terminates with the script string to be executed")

        // load the executor to be used
        def execName = getExecutorName(taskConfig) ?: 'local'
        def execClass = loadExecutorClass(execName)

        if( !isTypeSupported(script.type, execClass) ) {
            log.warn "Process '$name' cannot be executed by '$execName' executor -- Using 'local' executor instead"
            execName = 'local'
            execClass = LocalExecutor.class
        }

        def execObj = execClass.newInstance()
        // inject the task configuration into the executor instance
        execObj.taskConfig = taskConfig
        execObj.session = session
        execObj.name = execName
        execObj.init()

        // create processor class
        def processorClass = taskConfig.merge ? MergeTaskProcessor : ParallelTaskProcessor
        def result = processorClass.newInstance( execObj, session, this, taskConfig, script )
        return result

    }

    /**
     * Find out the 'executor' to be used in the process definition or in teh session configuration object
     *
     * @param taskConfig
     */
    private getExecutorName(Map taskConfig) {
        log.trace ">> taskConfig $taskConfig"

        // create the processor object
        def result = taskConfig.executor?.toString()

        if( !result ) {
            if( session.config.executor instanceof String ) {
                result = session.config.executor
            }
            else if( session.config.executor?.name instanceof String ) {
                result = session.config.executor.name
            }
        }

        log.debug "<< taskConfig executor: $result"
        return result
    }

    /**
     * Method to which is mapped the *process* declaration when using the following syntax:
     *    <pre>
     *        process myProcess( option: value[, [..]] ) &#123;
     *        ..
     *        &#125;
     *    </pre>
     *
     * @param args The process options specified in the parenthesis after the process name, as shown in the above example
     * @param name The name of the process, e.g. {@code myProcess} in the above example
     * @param body The body of the process declaration. It holds all the process definitions: inputs, outputs, code, etc.
     */
    protected process( Map<String,?> args, String name, Closure body ) {

        // create the process
        taskProcessor = createProcessor(name, body, args)
        if( isTest )
            return taskProcessor

        // launch it
        result = taskProcessor.run()
    }

    /**
     * Method to which is mapped the *process* declaration.
     *
     *    <pre>
     *        process myProcess( option: value[, [..]] ) &#123;
     *        ..
     *        &#125;
     *    </pre>
     * @param name The name of the process, e.g. {@code myProcess} in the above example
     * @param body The body of the process declaration. It holds all the process definitions: inputs, outputs, code, etc.
     */
    protected process( String name, Closure body ) {

        // create the process
        taskProcessor = createProcessor(name, body )
        if( isTest )
            return taskProcessor

        // launch it
        result = taskProcessor.run()
    }


    /*
     * Map the executor class to its 'friendly' name
     */
    static executorsMap = [
            'nope': NopeExecutor.name,
            'local': LocalExecutor.name,
            'sge':  SgeExecutor.name,
            'oge':  SgeExecutor.name,
            'lsf': LsfExecutor.name,
            'slurm': SlurmExecutor.name,
            'dnanexus': 'nextflow.executor.DnaNexusExecutor',
            'hazelcast': 'nextflow.executor.HzExecutor'
    ]

    @PackageScope
    static Class<? extends AbstractExecutor> loadExecutorClass(String executorName) {
        log.debug ">> processorType: $executorName"
        def className = executorName ? executorsMap[ executorName?.toLowerCase()  ] : LocalExecutor.name

        // if the className is empty (because the 'processorType' does not map to any class, fallback to the 'processorType' itself)
        if( !className ) {
            className = executorName
        }

        log.debug "Loading executor class: ${className}"
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className) as Class<AbstractExecutor>
        }
        catch( Exception e ) {
            throw new IllegalArgumentException("Cannot find a valid class for specified executor: '${executorName}'")
        }

    }


    @PackageScope
    static boolean isTypeSupported( ScriptType type, executor ) {


        if( executor instanceof AbstractExecutor ) {
            executor = executor.class
        }

        if( executor instanceof Class ) {
            def annotation = executor.getAnnotation(SupportedScriptTypes)
            if( !annotation )
                throw new IllegalArgumentException("Specified argument is not a valid executor class: $executor -- Missing 'SupportedScriptTypes' annotation")

            return type in annotation.value()
        }

        throw new IllegalArgumentException("Specified argument is not a valid executor class: $executor")
    }



}
