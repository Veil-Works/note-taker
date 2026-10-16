"""General coherence-preserving consolidation; no labels or case-specific rules."""
import itertools
import numpy as np
from grouping_experiment import jaccard


def consolidate(groups, matrix, threshold, overlap=.5):
    """Merge only overlapping cores whose full union still clears the original threshold."""
    groups = sorted(set(map(frozenset,groups)),key=lambda g:tuple(sorted(g)))
    while True:
        choices=[]
        for i,a in enumerate(groups):
            for j in range(i+1,len(groups)):
                b=groups[j]
                if jaccard(a,b)<overlap:
                    continue
                union=sorted(a|b)
                values=[matrix[x-1,y-1] for x,y in itertools.combinations(union,2)]
                if min(values)>=threshold:
                    choices.append((-jaccard(a,b),-float(np.mean(values)),tuple(union),i,j))
        if not choices:
            return groups
        _,_,_,i,j=min(choices)
        groups=[g for k,g in enumerate(groups) if k not in (i,j)]+[groups[i]|groups[j]]
        groups=sorted(set(groups),key=lambda g:tuple(sorted(g)))

