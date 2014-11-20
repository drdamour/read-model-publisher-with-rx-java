package com.bodybuilding.commerce;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.HashSet;


public class Matchers {

    public static <T> org.hamcrest.Matcher<T[]> hasDuplicateInArray() {
        return IsArrayWithDuplicates.<T>array();
    }

    public static class IsArrayWithDuplicates<T> extends TypeSafeMatcher<T[]> {

        public IsArrayWithDuplicates() {

        }

        @Override
        public boolean matchesSafely(T[] array) {
            HashSet aSet = new HashSet();
            aSet.addAll(Arrays.asList(array));

            return array.length > aSet.size();
        }

        @Override
        public void describeMismatchSafely(T[] actual, Description mismatchDescription) {
            mismatchDescription.appendText("TODO: explain what dupes there are");
        }

        @Override
        public void describeTo(Description description) {

            description.appendText("duplicates in array");

        }



        /**
         * Creates a matcher that matches arrays whose elements are satisfied by the specified matchers.  Matches
         * positively only if the number of matchers specified is equal to the length of the examined array and
         * each matcher[i] is satisfied by array[i].
         * <p/>
         * For example:
         * <pre>assertThat(new Integer[]{1,2,3}, is(array(equalTo(1), equalTo(2), equalTo(3))))</pre>
         *
         */
        @Factory
        public static <T> IsArrayWithDuplicates<T> array() {
            return new IsArrayWithDuplicates<T>();
        }

    }
}
