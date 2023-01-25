package org.mitre.thor.network.links;

import org.mitre.thor.network.nodes.Node;

public class Link {
    public static int idCount = 0;

    public final Node child;
    public final Node parent;
    public final int id;

    public String mathematicaColor = null;

    public Link(Node child, Node parent){
        this.child = child;
        this.parent = parent;

        id = ++idCount;
    }

    @Override
    public String toString() {
        return child.name + " -> " + parent.name;
    }
}
