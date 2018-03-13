package com.capgemini.cobigen.openapiplugin.matcher;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capgemini.cobigen.api.exception.CobiGenRuntimeException;
import com.capgemini.cobigen.api.exception.InvalidConfigurationException;
import com.capgemini.cobigen.api.extension.MatcherInterpreter;
import com.capgemini.cobigen.api.to.MatcherTo;
import com.capgemini.cobigen.api.to.VariableAssignmentTo;
import com.capgemini.cobigen.openapiplugin.model.EntityDef;

/**
 * Matcher for internal OpenAPI model.
 */
public class OpenAPIMatcher implements MatcherInterpreter {

    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(OpenAPIMatcher.class);

    /** Supported matcher types */
    private enum MatcherType {
        /** Element type */
        ELEMENT
    }

    /** Matcher variable types */
    private enum VariableType {
        /** Constant assignment */
        CONSTANT,
        /** Object property extraction */
        PROPERTY,
        /** Root package name for generation */
        ROOTPACKAGE,
        /** Object property extraction */
        ATTRIBUTE
    }

    @Override
    public boolean matches(MatcherTo matcher) {
        try {
            MatcherType matcherType = Enum.valueOf(MatcherType.class, matcher.getType().toUpperCase());
            switch (matcherType) {
            case ELEMENT:
                // to lower case to prevent from simple error cases
                return matcher.getTarget().getClass().getSimpleName().toLowerCase()
                    .equals(matcher.getValue().toLowerCase());
            default:
                break;
            }
        } catch (IllegalArgumentException e) {
            LOG.info("Matcher type '{}' not registered --> no match!", matcher.getType());
        }

        return false;
    }

    @Override
    public Map<String, String> resolveVariables(MatcherTo matcher, List<VariableAssignmentTo> variableAssignments)
        throws InvalidConfigurationException {

        Map<String, String> resolvedVariables = new HashMap<>();
        for (VariableAssignmentTo va : variableAssignments) {
            try {
                VariableType variableType = Enum.valueOf(VariableType.class, va.getType().toUpperCase());
                switch (variableType) {
                case CONSTANT:
                    resolvedVariables.put(va.getVarName(), va.getValue());
                    break;
                case ROOTPACKAGE:
                    String rootPackage = getRootPackage(matcher.getTarget());

                    // If there is no "x-rootPackage" on the info, then set the default value
                    if (rootPackage == null) {
                        resolvedVariables.put(va.getVarName(), va.getValue());
                    } else {
                        resolvedVariables.put(va.getVarName(), rootPackage);
                    }
                    break;
                case ATTRIBUTE:
                    Class<?> targetObject = matcher.getTarget().getClass();
                    try {
                        Field field = targetObject.getDeclaredField("extensionProperties");
                        field.setAccessible(true);
                        Object extensionProperties = field.get(matcher.getTarget());

                        String attributeValue = getExtendedProperty(extensionProperties, va.getValue());

                        resolvedVariables.put(va.getVarName(), attributeValue);
                    } catch (NoSuchFieldException | SecurityException e) {
                        LOG.warn(
                            "The property {} was requested in a variable assignment although the input does not provide this property. Setting it to null",
                            matcher.getValue());
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new CobiGenRuntimeException(
                            "This is a programming error, please report an issue on github", e);
                    }
                    break;
                case PROPERTY:
                    Class<?> target = matcher.getTarget().getClass();
                    try {
                        Field field = target.getDeclaredField(va.getValue());
                        field.setAccessible(true);
                        Object o = field.get(matcher.getTarget());

                        resolvedVariables.put(va.getVarName(), o.toString());
                    } catch (NoSuchFieldException | SecurityException e) {
                        LOG.warn(
                            "The property {} was requested in a variable assignment although the input does not provide this property. Setting it to null",
                            matcher.getValue());
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new CobiGenRuntimeException(
                            "This is a programming error, please report an issue on github", e);
                    }
                    break;
                }
            } catch (IllegalArgumentException e) {
                throw new CobiGenRuntimeException(
                    "Matcher or VariableAssignment type " + matcher.getType() + " not registered!", e);
            }

        }
        return resolvedVariables;
    }

    /**
     * Tries to cast the object "extensionProperties" to a map like the one defined in
     * {@link com.capgemini.cobigen.openapiplugin.model.EntityDef} . <br>
     * <br>
     * Additionally, tries to get a value of that map using the key passed as parameter.
     * @param object
     *            to be cast to a Map
     * @param key
     *            to search in the Map
     * @return the value of that key, and if nothing was found, an empty string
     */
    private String getExtendedProperty(Object extensionProperties, String key) {
        if (extensionProperties instanceof Map<?, ?>) {
            Map<String, Object> properties = (Map<String, Object>) extensionProperties;
            if (properties.containsKey(key)) {
                return properties.get(key).toString();
            } else {
                return "";
            }

        } else {
            throw new IllegalArgumentException("Unknown input object of type " + extensionProperties.getClass()
                + " in matcher execution. This may be a programming error, please report an issue on github");
        }
    }

    /**
     * Tries to get the root package name of the OpenAPI file.
     * @param target
     * @return If a root package name was found, returns the value. If not, returns null
     */
    private String getRootPackage(Object target) {
        if (target instanceof EntityDef) {
            EntityDef entity = (EntityDef) target;
            if (entity.getRootPackage() != null) {
                return entity.getRootPackage();
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException(
                "Unknown input object of type " + target.getClass() + " in matcher execution.");
        }
    }

}
