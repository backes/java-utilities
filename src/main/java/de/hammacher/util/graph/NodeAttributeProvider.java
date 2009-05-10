package de.hammacher.util.graph;

import java.util.Map;


public interface NodeAttributeProvider<NodeType> {

	Map<String, String> getAttributes(NodeType node);

}
