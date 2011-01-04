package com.danga.squeezer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Reflection utility methods
 * @author kaa
 *
 */
public class ReflectUtil {

	/**
	 * <p>
	 * Return the type parameter at the given position for the given class in the given superclass as a Class.<br>
	 * The method returns null if the class can't be resolved.
	 * </p>
	 * @param currentClass
	 * @param base
	 * @param genericArgumentNumber
	 * @return The generic type parameter at the given position as a class or null.
	 */
	public static Class<? extends Object> getGenericClass(Class<? extends Object> currentClass, Class<? extends Object> base, int genericArgumentNumber) {
		Type[] genericTypes = genericTypeResolver(currentClass, base);
		Type type = genericArgumentNumber < genericTypes.length ? genericTypes[genericArgumentNumber] : null;

		if (type instanceof Class<?>)
			return (Class<?>)type;
		return null;
	}

	/**
	 * <p>
	 * Resolve the types in the given generic superclass, the given class is declared with.<br>
	 * If the types can't be resolved an empty array is returned.<br>
	 * If any of the generic types from the parent can't be resolved it is set to null.
	 * <p>
	 * @param currentClass The current class that we should look for Generics in
	 * @param base Search upwards in the hierarchy until we pass this parent Class (which may go up all the way until {@link Object})
	 * @return List of types used in the declaration of currentClass.
	 */
	public static Type[] genericTypeResolver(Class<? extends Object> currentClass, Class<? extends Object> base) {
		Type[] actualTypeArguments = null;
		int[] genericTypesMap = null;

		while (!currentClass.isAssignableFrom(base)) {
			if (base.isInterface())
				for (Type iType : currentClass.getGenericInterfaces())
					if (iType instanceof ParameterizedType) {
						ParameterizedType pType = (ParameterizedType) iType;
						if (base.equals(pType.getRawType())) {
							if (actualTypeArguments == null)
								return pType.getActualTypeArguments();

							Type[] actualTypes = new Type[genericTypesMap.length];
							for (int i = 0; i < genericTypesMap.length; i++)
								actualTypes[i] = (genericTypesMap[i] < actualTypeArguments.length) ? actualTypeArguments[genericTypesMap[i]] : null;
							return actualTypes;
						}
					}


			Type type = currentClass.getGenericSuperclass();
			if (type instanceof ParameterizedType) {
				if (actualTypeArguments == null) {
					actualTypeArguments = ((ParameterizedType)type).getActualTypeArguments();
					genericTypesMap = new int[actualTypeArguments.length];
					for (int i = 0; i < actualTypeArguments.length; i++)
						genericTypesMap[i] = i;
				}
				else {
					TypeVariable<?>[] typeParameters = currentClass.getTypeParameters();
					Type[] actualTypes = ((ParameterizedType)type).getActualTypeArguments();
					int[] newGenericTypesMap = new int[actualTypes.length];
					for (int i = 0; i < actualTypes.length; i++) {
						newGenericTypesMap[i] = actualTypeArguments.length;
						for (int j = 0; j < typeParameters.length; j++) {
							if (actualTypes[i].equals(typeParameters[genericTypesMap[j]])) {
								newGenericTypesMap[i] = j;
								break;
							}
						}
					}
					genericTypesMap = newGenericTypesMap;
				}
			} else
				break;
			currentClass = currentClass.getSuperclass();
		}

		Type[] genericTypes = new Type[genericTypesMap.length];
		for (int i = 0; i < genericTypesMap.length; i++)
			genericTypes[i] = (genericTypesMap[i] < actualTypeArguments.length) ? actualTypeArguments[genericTypesMap[i]] : null;
		return genericTypes;
	}

}
