package org.mitre.thor.network.attack;

import java.util.ArrayList;
import java.util.List;

public class OrReqRule implements IReqRule{
    @Override
    public boolean isOption(Decision decision, AttackChain chain) {
        List<String> reqs = new ArrayList<>(decision.getRequirements());
        if (reqs.isEmpty()) return true;
        for (String req : reqs) {
            if (chain.containsRouteFullID(req)) return true;
        }
        return false;
    }
}
