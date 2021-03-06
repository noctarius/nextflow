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

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.codehaus.groovy.runtime.GStringImpl

/**
 * Presents a variable definition in the script context.
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Canonical
class TokenVar {

    /** The variable name */
    String name

}

/**
 *  A token used by the DSL to identify a 'file' declaration in a 'set' parameter, for example:
 *      <pre>
 *      input:
 *      set( file('name'), ... )
 *      </pre>
 *
 */
class TokenFileCall {

    final Object target

    TokenFileCall( value ) {
        this.target = value
    }

}

/**
 * An object of this class replace the {@code stdin} token in input map declaration. For example:
 * <pre>
 * input:
 *   map( stdin, .. ) from x
 * </pre>
 *
 * @see nextflow.ast.NextflowDSLImpl
 * @see SetInParam#bind(java.lang.Object[])
 */
class TokenStdinCall { }

/**
 * An object of this class replace the {@code stdout} token in input map declaration. For example:
 * <pre>
 * input:
 *   map( stdout, .. ) into x
 * </pre>
 *
 * @see nextflow.ast.NextflowDSLImpl
 * @see SetOutParam#bind(java.lang.Object[])
 */
class TokenStdoutCall { }

/**
 * Token used by the DSL to identify a environment variable declaration, like this
 *     <pre>
 *     input:
 *     set( env(X), ... )
 *     <pre>
 */
@Canonical
class TokenEnvCall {
    String name
}


/**
 * This class is used to identify a 'val' when used like in this example:
 * <pre>
 *  input:
 *  set ( val(x), ...  )
 *
 *  output:
 *  set( val(y), ...  )
 *
 * </pre>
 *
 */
@Canonical
class TokenValCall {
    String name
}


/**
 * This class is used to replace a GString usage in the output file declaration, for example:
 *
 *     <pre>
 *         output:
 *         file "${x}.txt" into something
 *     </pre>
 *
 */
@Canonical
class TokenGString {
    String text
    List<String> strings
    List<String> valNames

    String resolve( Closure map ) {
        def values = valNames.collect { String it -> map(it) }
        return new GStringImpl(values as Object[], strings as String[]).toString()
    }
}

/**
 * Holds process script meta-data
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class TaskBody {

    Closure closure

    String source

    ScriptType type

    Set<TokenValRef> valRefs

    TaskBody( Closure closure, String source, boolean scriptlet ) {
        this.closure = closure
        this.source = source
        this.type = scriptlet ? ScriptType.SCRIPTLET : ScriptType.GROOVY
    }

    TaskBody( Closure closure, String source, boolean scriptlet, TokenValRef... values ) {
        this(closure, source, scriptlet)
        this.valRefs = values != null ? values as Set : new HashSet<>()
    }

    String toString() {
        "TaskBody[closure: $closure; variables: $valRefs]"
    }

    /**
     * @return The list of variable names references by the user script
     */
    Set<String> getValNames() {
        valRefs *. name
    }
}


@ToString
@EqualsAndHashCode
class TokenValRef {
    String name
    int lineNum
    int colNum

    TokenValRef( String name, int lineNum = -1, int colNum = -1 ) {
        this.name = name
        this.lineNum = lineNum
        this.colNum = colNum
    }
}
