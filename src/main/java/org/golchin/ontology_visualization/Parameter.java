package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@AllArgsConstructor
@Getter
public class Parameter<T> {
    private final String name;
    private final String description;
    private final Set<T> possibleValues;

    public static Collection<Map<Parameter<?>, Object>> getParameterCombinations(List<Parameter<?>> parameters) {
        List<Map<Parameter<?>, Object>> result = new ArrayList<>();
        getParameterCombinationsHelper(result, parameters, 0);
        return result;
    }

    private static void getParameterCombinationsHelper(Collection<Map<Parameter<?>, Object>> result,
                                                       List<Parameter<?>> parameters,
                                                       int index) {
        if (parameters.isEmpty())
            return;
        if (index >= parameters.size() - 1) {
            Parameter<?> parameter = parameters.get(parameters.size() - 1);
            for (Object possibleValue : parameter.getPossibleValues()) {
                Map<Parameter<?>, Object> map = new HashMap<>();
                map.put(parameter, possibleValue);
                result.add(map);
            }
            return;
        }
        getParameterCombinationsHelper(result, parameters, index + 1);
        Parameter<?> parameter = parameters.get(index);
        List<Map<Parameter<?>, Object>> newResult = new ArrayList<>();
        for (Map<Parameter<?>, Object> map : result) {
            for (Object possibleValue : parameter.getPossibleValues()) {
                Map<Parameter<?>, Object> copy = new HashMap<>(map);
                copy.put(parameter, possibleValue);
                newResult.add(copy);
            }
        }
        result.clear();
        result.addAll(newResult);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter<?> parameter = (Parameter<?>) o;
        return Objects.equals(name, parameter.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
