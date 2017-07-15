package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("WeakerAccess")
public class StratifierKey extends ArrayList<Object> implements Comparable<StratifierKey> {

    private static final long serialVersionUID = 1L;

    public StratifierKey() {
    }

    public StratifierKey(final Collection<?> c) {
        super(c);
    }

    @Override
    @SuppressWarnings({"unchecked", "NullableProblems"})
    public int compareTo(final StratifierKey o) {
        int compare = this.size() - o.size();
        if (compare != 0)
            return compare;
        for (int i = 0; i < this.size(); i++) {
            compare = ((Comparable<Object>)o.get(i)).compareTo(this.get(i));
            if (compare != 0)
                return compare;
        }
        return 0;
    }
}
