---
layout: default
title: Token substitution in perfmonconfig.xml
permalink: /documentation/userguide/tokensubstitution/
showInHeader: false
---

#Token Substitution:

When supplying the token DEFAULT_INTERVAL, as shown below, Perfmon4j will first search for a system property with the name “DEAULT_INTERVAL”, if it is not found it will look to for an environment variable.

~~~~ xml
<appender name='webrequest-appender' className='org.perfmon4j.TextAppender' 
	interval='${DEFAULT_INTERVAL}'>
   <attribute name='medianCalculator'>factor=10</attribute>		
</appender>
~~~~

If you would prefer that environmental variables take precidence you can append the the prefex "env." on the token, as shown below. Perfmon4j will then look to resolve the variable "DEFAULT_INTERVAL" by first looking at the environmental variables, and then if not found, looking at system properties.

~~~~ xml
<appender name='webrequest-appender' className='org.perfmon4j.TextAppender' 
	interval='${env.DEFAULT_INTERVAL}'>
   <attribute name='medianCalculator'>factor=10</attribute>		
</appender>
~~~~

