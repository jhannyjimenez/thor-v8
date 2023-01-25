package org.mitre.thor.network.nodes;

public class SisterActivity extends Activity {
    //If this node is a copy
    public Activity origin = null;

    public SisterActivity(String name) {
        super(name);
    }
}
