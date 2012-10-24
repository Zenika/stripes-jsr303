/* 
 * Copyright (c) 2012 Zenika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zenika.stripes.contrib.jsr303validator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;

/**
 * An Interceptor implementation to support the Java API for JavaBean Validation (JSR-303).
 * 
 * @author Florian Hussonnois (Zenika)
 * @author Yohan Legat (Zenika)
 *
 */
@Intercepts({ LifecycleStage.BindingAndValidation })
public class JSR303ValidationInterceptor implements Interceptor {

	/**
	 * Private static members
	 */
	private static final Logger LOG = LoggerFactory.getLogger(JSR303ValidationInterceptor.class);
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * Executes JSR-303 validation on the {@link ActionBean} which is obtained from the {@link ExecutionContext}.
	 */
	public Resolution intercept(ExecutionContext ctx) throws Exception 
	{
		LOG.debug("Intercept for JSR303 validation");
		Resolution resolution = ctx.proceed();
		
		Method method = ctx.getHandler();
		if ( isValidationEventEnabled(method) ) 
		{
			ActionBean action = ctx.getActionBean();
			ActionBeanContext  abx = action.getContext();
			this.mapConstraintViolations(abx.getValidationErrors(),this.validate(action,  getGroupSequence(method)));
			
			if( !abx.getValidationErrors().isEmpty() ) resolution = abx.getSourcePageResolution();	
		}
		return resolution;

	}

	/**
	 * Returns the groups of validation which are bonded to the given method with the {@link EventGroups} annotation.
	 * 
	 * @param m The method that handle the current event.
	 */
	private List<Class<?>> getGroupSequence(Method m)
	{
		List<Class<?>> groups = new ArrayList<Class<?>>();
		groups.add(Default.class);
		EventGroups eventGroups = m.getAnnotation(EventGroups.class);
		LOG.debug("Is handler defines groups for validation?  "+ (eventGroups != null ? "true" : "false"));
		if (eventGroups != null) for (Class<?> value : eventGroups.groups()) groups.add(value);
		return groups;
	}
	
	/**
	 * Validates the {@link ActionBean} properties.
	 * 
	 * @param action The {@link ActionBean}
	 * @param groups The groups for validation
	 */
	private Set<ConstraintViolation<ActionBean>> validate(ActionBean action, List<Class<?>> groups) 
	{
		LOG.debug("Is validating the ActionBean properties");
		return getValidator().validate(action, groups.toArray(new Class[groups.size()]));
	}

	/**
	 * Maps a Set of @link{ConstraintViolations} to the {@link ValidationErrors}.
	 * 
	 * @param errors  {@link ValidationErrors}
	 * @param constraintViolations The Set of @link{ConstraintViolations}
	 */
	private void mapConstraintViolations(ValidationErrors errors, Set<ConstraintViolation<ActionBean>> constraintViolations) 
	{
		for (ConstraintViolation<? extends ActionBean> constraintViolation : constraintViolations) {
			SimpleError error = getSimpleError(constraintViolation);
			errors.add(error.getFieldName(), error);
		}
	}
	
	/**
	 * Returns a new {@link ConstraintViolation} based on the given {@link ConstraintViolation}.
	 * 
	 * @param constraintViolation A {@link ConstraintViolation}
	 * @return The {@link ConstraintViolation}.
	 */
	private SimpleError getSimpleError(ConstraintViolation<? extends ActionBean> constraintViolation)
	{
		SimpleError error = new SimpleError(constraintViolation.getMessage());
		error.setFieldName( constraintViolation.getPropertyPath().toString() );
		Object invalidValue = constraintViolation.getInvalidValue();
		if( invalidValue != null )
			error.setFieldValue(constraintViolation.getInvalidValue().toString() );
		LOG.debug("Adds a new simple error for the field:" + error.getFieldName());
		return error;
	}

	/**
	 * Checks whether the given {@link Method} handler has to perform properties validations.
	 * 
	 * @param m The method that handle the current event.
	 * @return a boolean.
	 */
	private boolean isValidationEventEnabled(Method m) 
	{
		return m != null && m.getAnnotation(DontValidate.class) == null;
	}


	/**
	 * Returns a new {@link Validator} for java bean validation.
	 * 
	 * @return @see(http://docs.oracle.com/javaee/6/api/javax/validation/Validator.html)
	 */
	private Validator getValidator() 
	{
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		return factory.getValidator();
	}

}
