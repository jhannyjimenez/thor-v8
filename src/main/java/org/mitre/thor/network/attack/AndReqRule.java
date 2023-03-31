package org.mitre.thor.network.attack;

import java.util.ArrayList;
import java.util.List;

public class AndReqRule implements IReqRule{
    @Override
    public boolean isOption(Decision decision, Route route, AttackChain chain) {
        List<String> reqs = new ArrayList<>(decision.getRequirements());
        if (reqs.isEmpty()) return true;
        String rID = route.getFullId();
        if (reqs.contains(rID)) {
            reqs.remove(rID);
            for (String req : reqs) {
                if (!chain.containsRouteFullID(req)) return false;
            }
            return true;
        }
        return false;
    }
}
