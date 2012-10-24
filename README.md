stripes-jsr303
==============

A Stripes Interceptor implementation to support the Java API for JavaBean Validation (JSR-303).

To use it just add the following lines into to the Stripes filter which is defined into your web.xml :
 
	<init-param>
		<param-name>Interceptor.Classes</param-name>
		<param-value>
			com.zenika.stripes.contrib.jsr303validator.JSR303ValidationInterceptor
		</param-value>
	</init-param>