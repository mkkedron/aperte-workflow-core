package pl.net.bluesoft.rnd.util;

import pl.net.bluesoft.rnd.processtool.ProcessToolContext;
import pl.net.bluesoft.rnd.processtool.model.IAttributesProvider;
import pl.net.bluesoft.rnd.processtool.model.ProcessInstance;

import java.util.*;

/**
 * Util for steps
 *
 * @author: "mpawlak@bluesoft.net.pl"
 */
public class StepUtil {
    public static String extractVariable(String variableCode, ProcessToolContext ctx, ProcessInstance pi) {
        if (variableCode == null)
            return null;

        variableCode = variableCode.trim();

        /* If key is variable #{variableName}, extract it */
        if (variableCode.matches("#\\{.*\\}")) {
            String variableName = variableCode.replaceAll("#\\{(.*)\\}", "$1");
            variableName = pi.getSimpleAttributeValue(variableName);

            return variableName;
        }
        /* Otherwise variableCode == key */
        else {
            return pi.getSimpleAttributeValue(variableCode);
        }
    }

	/**
	 * Replaces each occurence of #{attr} with value of attribute attr
	 */
	public static String substituteVariables(String pattern, final IAttributesProvider pi) {
		return PlaceholderUtil.expand(pattern, new PlaceholderUtil.ReplacementCallback() {
			@Override
			public String getReplacement(String placeholderName) {
				String largePrefix = "L:";
				if (placeholderName.startsWith(largePrefix)) {
					return pi.getSimpleLargeAttributeValue(placeholderName.substring(largePrefix.length()));
				}
				return pi.getSimpleAttributeValue(placeholderName);
			}
		});
	}

    /**
     * Evaluate the given query string to a list of simple attributes.
     *
     * @param query
     * @return
     */
    public static Map<String, String> evaluateQuery(String query)
    {
        if(query == null)
            return null;

        Map<String, String> attributes = new HashMap<String, String>();
        String[] parts = query.split("[,;]");
        for (String part : parts) {
            String[] assignment = part.split("[:=]");
            if (assignment.length != 2)
                continue;

            if (assignment[1].startsWith("\"") && assignment[1].endsWith("\""))
                assignment[1] = assignment[1].substring(1, assignment[1].length() - 1);

            String key = assignment[0];
            String value = assignment[1];
            attributes.put(key, value);
        }
        return attributes;
    }

    public static List<String> evaluateList(String query)
    {
        if(query == null) {
			return Collections.emptyList();
		}

		List<String> attributes = new ArrayList<String>();
        String[] parts = query.split("[,;]");

        for (String part : parts) {
            attributes.add(part);
        }
        return attributes;
    }
}
