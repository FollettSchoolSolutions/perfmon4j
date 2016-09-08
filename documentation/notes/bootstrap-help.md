---
layout: page
title: Bootstrap-Help
permalink: /documentation/notes/bootstrap/
showInHeader: false
---
## Important Notes for bootstrap implementation.

If you want to instrument any of the jvm classes (i.e. java.lang.String) you must include 
the directory that contains the perfmon4j agent in the list of endorsed folders.  This is accomplished
by setting the system property 'java.endorsed.dirs' when starting java.

For example to instrument java.lang.String you would add the following to your java command line:

* javaagent:/*\<my folder containing perfmon4j\>*/perfmon4j.jar=-ejava.lang.Throwable,-ejava.lang.String,-btrue,*\<any additional perfmon4j parameters\>*&nbsp;&nbsp;&nbsp;-Djava.endorsed.dirs=*\<my folder containing perfmon4j\>* 

