package org.golchin.ontology_visualization;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@AllArgsConstructor
@Getter
public class Parameter<T> {
    private final String name;
    private final Set<T> possibleValues;

    public static Collection<Map<String, Object>> getParameterCombinations(List<Parameter<?>> parameters) {
        List<Map<String, Object>> result = new ArrayList<>();
        getParameterCombinationsHelper(result, parameters, 0);
        return result;
    }

    private static void getParameterCombinationsHelper(Collection<Map<String, Object>> result,
                                                       List<Parameter<?>> parameters,
                                                       int index) {
        if (parameters.isEmpty())
            return;
        if (index >= parameters.size() - 1) {
            Parameter<?> parameter = parameters.get(parameters.size() - 1);
            for (Object possibleValue : parameter.getPossibleValues()) {
                Map<String, Object> map = new HashMap<>();
                map.put(parameter.getName(), possibleValue);
                result.add(map);
            }
            return;
        }
        getParameterCombinationsHelper(result, parameters, index + 1);
        Parameter<?> parameter = parameters.get(index);
        List<Map<String, Object>> newResult = new ArrayList<>();
        for (Map<String, Object> map : result) {
            for (Object possibleValue : parameter.getPossibleValues()) {
                Map<String, Object> copy = new HashMap<>(map);
                copy.put(parameter.getName(), possibleValue);
                newResult.add(copy);
            }
        }
        result.clear();
        result.addAll(newResult);
    }
}
