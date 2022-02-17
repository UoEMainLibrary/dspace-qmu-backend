/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.app.rest.model.hateoas.NBSourceResource;
import org.hamcrest.Matcher;

/**
 * Matcher related to {@link NBSourceResource}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class NBSourceMatcher {

    private NBSourceMatcher() { }

    public static Matcher<? super Object> matchNBSourceEntry(String key, int totalEvents) {
        return allOf(
            hasJsonPath("$.type", is("nbsource")),
            hasJsonPath("$.id", is(key)),
            hasJsonPath("$.totalEvents", is(totalEvents))
        );
    }


    public static Matcher<? super Object> matchNBSourceEntry(String key) {
        return allOf(
            hasJsonPath("$.type", is("nbsource")),
            hasJsonPath("$.id", is(key))
        );
    }

}
