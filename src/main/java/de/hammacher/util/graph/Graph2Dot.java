package de.hammacher.util.graph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import de.hammacher.util.Pair;
import de.hammacher.util.graph.Graph.Node;

public class Graph2Dot<NodeType extends Node<NodeType>> {

	private String graphName = "graph";
	private String nodeShape;
	private final Map<String, String> graphAttributes = new HashMap<String, String>(4);
	private EdgeLabelProvider<? super NodeType> edgeLabelProvider = null;
	private GraphPartitioner<NodeType> graphPartitioner = null;
	private NodeAttributeProvider<? super NodeType> nodeAttributeProvider = null;

	/**
	 * Sets the node attribute provider for this exporter, which is asked for the attributes
	 * of each exported node.
	 * @param nodeAttributeProvider the new node attribute provider
	 */
	public void setNodeAttributeProvider(
			NodeAttributeProvider<? super NodeType> nodeAttributeProvider) {
		this.nodeAttributeProvider = nodeAttributeProvider;
	}

	/**
	 * Sets a new edge label provider for this exporter.
	 * @param edgeLabelProvider the new edge label provider
	 */
	public void setEdgeLabelProvider(EdgeLabelProvider<? super NodeType> edgeLabelProvider) {
		this.edgeLabelProvider = edgeLabelProvider;
	}

	/**
	 * Sets a new graph partitioner object for this exporter.
	 *
	 * If the graph partitioner is <code>null</code> (the default value), then the graph
	 * is not split into subgraphs.
	 *
	 * @param graphPartitioner the new graph partioner
	 */
	public void setGraphPartitioner(GraphPartitioner<NodeType> graphPartitioner) {
		this.graphPartitioner = graphPartitioner;
	}

	/**
	 * @see #export(Graph, PrintStream)
	 * @throws IOException if the supplied file could not be opened, or there was an error
	 *                     while writing into that file
	 */
	public void export(Graph<? extends NodeType> graph, File dotFile) throws IOException {
		FileOutputStream fos = new FileOutputStream(dotFile);
		try {
			PrintStream out = new PrintStream(fos);
			export(graph, out);
			if (out.checkError())
				throw new IOException("Error writing dot file");
		} finally {
			fos.close();
		}
	}

	/**
	 * Exports the given graph to the PrintStream, using the supplied edge label
	 * provider, and the graph partitioner.
	 *
	 * @param graph the graph to export
	 * @param out the print stream to output the dot document
	 * @throws IllegalArgumentException if one of the nodes successors is not
	 *                                  contained in the nodes
	 */
	public void export(Graph<? extends NodeType> graph, PrintStream out) {
		out.format("digraph %s {%n", quoteDotString(this.graphName));
		if (!this.graphAttributes.isEmpty()) {
			out.println();
			for (Map.Entry<String, String> e: this.graphAttributes.entrySet())
				out.format("  %s=%s%n", e.getKey(), quoteDotString(e.getValue()));
		}
		if (this.nodeShape == null)
			out.println();
		else
			out.format("%n  node [shape=%s]%n%n", quoteDotString(this.nodeShape));

		final Map<NodeType, Integer> nodeNumbers =
			new HashMap<NodeType, Integer>();
		if (this.graphPartitioner == null) {
			for (NodeType node : graph.getNodes())
				getNodeNumber(nodeNumbers, node, out, false);
		} else {
			Iterator<Pair<String, Collection<? extends NodeType>>> subGraphIterator =
				this.graphPartitioner.subGraphIterator();
			while (subGraphIterator.hasNext()) {
				Pair<String, Collection<? extends NodeType>> subGraph =
						subGraphIterator.next();
				if (subGraph.getFirst() != null) {
					out.format("  subgraph cluster_%s {%n    label=%s%n%n",
						subGraph.getFirst().replaceAll("[^a-zA-Z0-9\\-_]", "_"),
						quoteDotString(subGraph.getFirst()));
				}
				for (NodeType node : subGraph.getSecond())
					getNodeNumber(nodeNumbers, node, out, subGraph.getFirst() != null);
				if (subGraph.getFirst() != null)
					out.format("  }%n");
			}
		}
		out.println();

		@SuppressWarnings("unchecked")
		NodeType[] nodes = (NodeType[]) nodeNumbers.keySet().toArray(new Node[nodeNumbers.size()]);

		Arrays.sort(nodes, new Comparator<Node<?>>() {
			public int compare(Node<?> o1, Node<?> o2) {
				return nodeNumbers.get(o1).compareTo(nodeNumbers.get(o2));
			}
		});

		for (NodeType node : nodes) {
			Integer nr1 = nodeNumbers.get(node);
			for (NodeType succ: node.getSuccessors()) {
				Integer nr2 = nodeNumbers.get(succ);
				if (nr2 == null)
					throw new IllegalArgumentException("Successor of a node not contained in the nodes.");
				String label = this.edgeLabelProvider == null ? null : this.edgeLabelProvider.getLabel(node, succ);
				if (label == null)
					out.format("  %d -> %d%n", nr1, nr2);
				else
					out.format("  %d -> %d [label=%s]%n", nr1, nr2, quoteDotString(label));
			}
		}

		out.format("}%n%n");
	}

	private Integer getNodeNumber(Map<NodeType, Integer> nodeNumbers, NodeType node,
			PrintStream out, boolean inSubGraph) {
		Integer nr = nodeNumbers.get(node);
		if (nr == null) {
			nr = nodeNumbers.size();
			nodeNumbers.put(node, nr);
			Map<String, String> attributes = this.nodeAttributeProvider == null
				? Collections.<String, String>emptyMap()
				: this.nodeAttributeProvider.getAttributes(node);
			if (attributes.isEmpty()) {
				out.format("%s%d [label=%s]%n", inSubGraph ? "    " : "  ", nr, quoteDotString(node.getLabel()));
			} else {
				out.format("%s%d [label=%s", inSubGraph ? "    " : "  ", nr, quoteDotString(node.getLabel()));
				for (Entry<String, String> attribute : attributes.entrySet()) {
					out.format(", %s=%s", attribute.getKey(), attribute.getValue());
				}
				out.format("]%n");
			}
		}
		return nr;
	}

	public void setGraphName(String newName) {
		this.graphName = newName;
	}

	public void setNodeShape(String newShape) {
		this.nodeShape = newShape;
	}

	/**
	 * Set a graph attribute
	 * @param key
	 * @param value
	 * @return the old value
	 */
	public String setGraphAttribute(String key, String value) {
		return this.graphAttributes.put(key, value);
	}

    /**
     * Quote the supplied string for output.
     *
     * @param input
     *            the string to be quoted
     * @return the input string, possibly enclosed in double quotes and with
     *         internal double quotes protected.
     */
    // essentially the agstrcanon function from libgraph (by S. C. North)
    public static String quoteDotString(String input) {
        int len;

        if (input == null || (len = input.length()) == 0) {
            return ("\"\"");
        }

        char[] buffer = new char[len+4];
        int bufPos = 1;
        boolean hasSpecial = false;
        boolean atStart = true;

        for (int isub = 0; isub < len; ++isub) {
        	char c = input.charAt(isub);
        	switch (c) {
        	case '"':
        	case '\\':
        		buffer = appendToBuffer(buffer, bufPos++, '\\');
                hasSpecial = true;
                atStart = false;
                break;

        	case '\r':
                continue;

        	case '\n':
        		if (!atStart) {
	        		buffer = appendToBuffer(buffer, bufPos++, '\\');
	        		buffer = appendToBuffer(buffer, bufPos++, 'n');
        		}
                continue;

            default:
            	if (atStart) {
            		if (Character.isWhitespace(c))
            			continue;
            		else
            			atStart = false;
            	}
            	if (!hasSpecial && !(Character.isLetter(c) || Character.isDigit(c)
            			|| Character.getType(c) == Character.LETTER_NUMBER))
                    hasSpecial = true;
	            break;
            }
    		buffer = appendToBuffer(buffer, bufPos++, c);
        }

        // trim from behind
        while (bufPos > 1 && Character.isWhitespace(buffer[bufPos-1]))
        	--bufPos;

        // if special characters are included and the string is NOT probably html,
        // then wrap it in double quotes
        if (!hasSpecial || (bufPos > 2 && buffer[1] == '<' && buffer[bufPos-1] == '>'))
        	return String.valueOf(buffer, 1, bufPos-1);

        buffer[0] = '"';
        buffer = appendToBuffer(buffer, bufPos++, '"');
        return String.valueOf(buffer, 0, bufPos);
    }

	private static char[] appendToBuffer(char[] buffer, int pos, char c) {
		assert pos >= 0 && pos <= buffer.length;
		if (pos == buffer.length) {
			char[] newBuffer = new char[buffer.length*3/2];
			System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
			return appendToBuffer(newBuffer, pos, c);
		}
		buffer[pos] = c;
		return buffer;
	}


}
