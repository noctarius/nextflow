.. _pipeline-page:

*****************
Pipeline script
*****************


The Nextflow scripting language is an extension of the Groovy programming language whose syntax has been
specialized to ease the writing of computational pipelines in a declarative manner.

This means that Nextflow can execute any Groovy piece of code or use any library for the Java JVM platform.

For a detailed description of the Groovy programming language, references these links:

* `Groovy User Guide <http://groovy.codehaus.org/User+Guide>`_
* `Groovy Cheat sheet <http://refcardz.dzone.com/refcardz/groovy>`_
* `Groovy in Action <http://www.manning.com/koenig2/>`_


Below you can find a crash course in the most important language constructs used in the Nextflow scripting language.


Language basics
==================


Hello world
------------

To print something is as easy as using the ``print`` or the ``println`` methods.
::

    println "Hello, World!"

The only difference between the two is that the ``println`` method appends implicitly a `new line` character
to the printed string.


Variables
----------

To define a variable, simply assign a value to it. For example::

    x = 1
    println x

    x = new java.util.Date()
    println x

    x = -3.1499392
    println x

    x = false
    println x

    x = "Hi"
    println x


Lists
------

A List object can be defined by placing the list items in square brackets. For example::

    myList = [1776, -1, 33, 99, 0, 928734928763]

You can access a given item in the list with square bracket notation (indexes start at 0)::

    println myList[0]

In order to get the length of the list use the ``size`` method::

    println myList.size()


Learn more about lists:

* `Groovy Lists tutorial <http://groovy.codehaus.org/Collections#Collections-Lists>`_
* `Groovy List SDK <http://groovy.codehaus.org/groovy-jdk/java/util/List.html>`_
* `Java List SDK <http://docs.oracle.com/javase/7/docs/api/java/util/List.html>`_


Maps
-----

Maps are used to store `associative arrays` or `dictionaries`. They are unordered collections of heterogeneous, named data.
For example::

    scores = [ "Brett":100, "Pete":"Did not finish", "Andrew":86.87934 ]


Note that each of the values stored in the map can be of a different type. Brett's is an integer, Pete's is a string,
and Andrew's is a floating point number.

We can access the values in a map in two main ways, as shown below::

    println scores["Pete"]
    println scores.Pete


To modify or add data to a map, the syntax is similar to adding values to list.
::

    scores["Pete"] = 3
    scores["Cedric"] = 120


Learn more about maps at the following links:

* `Groovy Maps tutorial <http://groovy.codehaus.org/Collections#Collections-maps>`_
* `Groovy Map SDK <http://groovy.codehaus.org/groovy-jdk/java/util/Map.html>`_
* `Java Map SDK <http://docs.oracle.com/javase/7/docs/api/java/util/Map.html>`_


.. _script-multiple-assignment:

Multiple assignment
----------------------

An array or a list object can used to assign multiple variables at once. For example::

    (a, b, c) = [10, 20, 'foo']
    assert a == 10 && b == 20 && c == 'foo'

The three variables on the left of the assignment operator are initialized by the correspondent item in the list.

Read more about `Multiple assignment <http://groovy.codehaus.org/Multiple+Assignment>`_ on the Groovy documentation.


Conditional Execution
----------------------

One of the most important features of any programming language is the ability to execute different code under
different conditions. The simplest way to do this is to use the ``if`` construct. For example::

    x = Math.random()
    if( x < 0.5 ) {
        println "You lost."
    }
    else {
        println "You won!"
    }



Strings
--------

Strings can be defined by delimiting them with a single-quote ``'`` or a double-quote ``"``.
Using either type of string allows you to use strings with quotations easily, as shown below::

    println "he said 'cheese' once"
    println 'he said "cheese!" again'


Strings may be concatenated with ``+``. For example::

    a = "world"
    print "hello " + a + "\n"


.. _string-interpolation:

String interpolation
----------------------

There is an important difference between single ``'`` and double ``"`` quoted strings.
Double quoted strings supports variable interpolations, while single quoted strings do not.

In practice, strings that are declared inside double-quotes can contain arbitrary variables prefixing them with the ``$`` character
or any expressions by using the ``${expression}`` syntax in a very similar way to Bash/shell scripts.
::

    foxtype = 'quick'
    foxcolor = ['b', 'r', 'o', 'w', 'n']
    println "The $foxtype ${foxcolor.join()} fox"

    x = 'Hello'
    println '$x + $y'

It prints::

    The quick brown fox
    $x + $y


Multi-line strings
-------------------

A block of text that span multiple lines can be defined by delimiting it with triple single or double quotes, as shown below::

    text = """
        hello there James
        how are you today?
        """

.. note:: Like before, multi-line strings delimited by double-quotes characters supports variable interpolation, while
   single-quote string do not.


As in Bash/shell scripts, when terminating a multi-line text block with a ``\`` character, the resulting string is
not broken up by `new line` character(s)::

    myLongCmdline = """ blastp \
                    -in $input_query \
                    -out $output_file \
                    -db $blast_database \
                    -html
                    """

    result = myLongCmdline.execute().text




.. _script-closure:

Closures
=========

In very few words a closure is a block of code that can be passed as an argument to a function.
Thus you can define a chunk of code and then pass it around as if it were a string or an integer.

More formally, you can create functions that are defined `first class objects`.

::

    square = { it * it }


The curly brackets around the expression ``it * it`` tells the script interpreter to treat this expression as code.
In this case, the designator ``it`` refers to whatever value is given to the function. Then this compiled function is
assigned to the variable `'square`` much like those above. So now we can do something like this::

    println square(9)

and get the value 81.


This is not very interesting until we find that we can pass this function ``square`` around as a method argument.
There are some built in functions that take a function like this as an argument. One example is the ``collect`` method on lists.
For example::

    [ 1, 2, 3, 4 ].collect(square)


This expression says, create an array with the values 1,2,3 and 4, then call the `collect` method, passing in the
closure we defined above. The collect method runs through each item in the array, calls the closure on the item,
then puts the result in a new array, resulting in:

    [ 1, 4, 9, 16 ]


For more methods that you can call with closures as arguments, see the `Groovy GDK documentation <http://groovy.codehaus.org/groovy-jdk/>`_.


By default closures take 1 parameter called ``it``, you can also create closures with named parameters.
For example the method ``Map.each()`` can take a closure with two variables, to which it binds the `key` and the associated `value`::


    printMapClosure = { key, value ->
        println "$key = $value"
    }

    [ "Yue" : "Wu", "Mark" : "Williams", "sudha" : "Kumari" ].each(printMapClosure)


Prints::


    Yue=Wu
    Mark=Williams
    Sudha=Kumari




A closure has another two important features. First it can access variables in the scope where it is defined and
so it can `interact` with them.

The second thing is that a closure can be defined in an `anonymous` manner, meaning that it is not given a name,
and is defined in the place where it needs to be used.

As an example showing both these features see the following code fragment::

    myMap = ["China": 1 , "India" : 2, "USA" : 3]

    result = 0
    myMap.keySet().each( { result+= myMap[it] } )

    println result



.. _script-regexp:

Regular expressions
====================

Regular expressions are the Swiss Army knife of text processing. They provide the programmer with the ability to match
and extract patterns from strings.

Regular expressions are supported by using the ``~/pattern/`` syntax and the ``=~`` and the ``==~`` operators.

Use the ``=~`` to check if there's any occurrence of a given pattern into a given string, thus::

    assert 'foo' =~ /foo/       // return TRUE
    assert 'foobar' =~ /foo/    // return TRUE


Use the ``==~`` to check whenever a string matches a given regular expression pattern.
::

    assert 'foo' ==~ /foo/       // return TRUE
    assert 'foobar' ==~ /foo/    // return FALSE


It is worth noting that the ``~`` operator creates a Java ``Pattern`` object from the given string,
while the ``=~`` creates a Java ``Matcher`` object.
::

    x = ~/abc/
    println x.class
    // prints java.util.regex.Pattern

    y = 'some string' =~ /abc/
    println y.class
    // prints java.util.regex.Matcher


Regular expression support is imported from Java. Java's regular expression language and API is documented in the
`Pattern Java documentation <http://download.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html>`_.


String replacements
--------------------

To replace pattern occurrences into a given string use the ``replaceFirst`` and ``replaceAll`` methods. For example::

     x = "colour".replaceFirst(/ou/, "o")
     println x
     // prints: color

     y = "cheesecheese".replaceAll(/cheese/,"nice")
     println y
     // prints: nicenice



Capturing groups
----------------

You can match a pattern that includes groups.  First create a matcher object with the ``=~`` operator.
Then, you can index the matcher object to find the matches, ``matcher[0]`` returns a list representing the first match
of the regular expression in the string. The first element is the string that matches the entire regular expression, and
the remaining elements are the strings that match each group.

Here's how it works::

    programVersion = '2.7.3-beta'
    m = programVersion =~ /(\d+)\.(\d+)\.(\d+)-?(.+)/

    assert m[0] ==  ['2.7.3-beta', '2', '7', '3', 'beta']
    assert m[0][1] == '2'
    assert m[0][2] == '7'
    assert m[0][3] == '3'
    assert m[0][4] == 'beta'



Applying some syntax sugar, you can do the same in just one line of code::

    programVersion = '2.7.3-beta'
    (full, major, minor, patch, flavor) = (programVersion =~ /(\d+)\.(\d+)\.(\d+)-?(.+)/)[0]

    println full    // 2.7.3-beta
    println major   // 2
    println minor   // 7
    println patch   // 3
    println flavor  // beta


Removing part of a string
-------------------------

You can subtract a part of a String value using a regular expression pattern. The first match found is
replaced with an empty String. For example::

    // define the regexp pattern
    wordStartsWithGr = ~/(?i)\s+Gr\w+/

    // apply and verify the result
    ('Hello Groovy world!' - wordStartsWithGr) == 'Hello world!'
    ('Hi Grails users' - wordStartsWithGr) == 'Hi users'



Remove first match of a word with 5 characters::

    assert ('Remove first match of 5 letter word' - ~/\b\w{5}\b/) == 'Remove  match of 5 letter word'


Remove first found numbers followed by a whitespace character::

    assert ('Line contains 20 characters' - ~/\d+\s+/) == 'Line contains characters'



Files and I/O
==============

::

    def dir = file('somedir')
    def cl = { Path f -> println f }
    dir.eachDir cl
    dir.eachFile cl
    dir.eachDirRecurse cl
    dir.eachFileRecurse cl
    dir.eachDirMatch(~/.*/, cl)
    dir.eachFileMatch(~/.*/, cl)