/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * LRUSketch.java
 * Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.streams.twitter;

import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.AbstractOptionHandler;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of the so called LRUSketch algorithm
 * for counting frequent items, actually String tokens in this version
 *
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * 
 * Modified for TfIdf
 *
 */
public class LRUSketch extends AbstractOptionHandler implements Sketch {

	private static final long serialVersionUID = 1L;
	
	public IntOption capacityOption = new IntOption("capacity", 'k', "Number of attributes to use", 1000);
	
	public FloatOption epsilonOption = new FloatOption("epsilon", 'e', "Epsilon to use", .01);
	
	public FloatOption deltaOption = new FloatOption("delta", 'd', "Delta to use", .01);
	
	public FloatOption lambdaOption = new FloatOption("lambda", 'l', "Lambda to use", .01);
	
	protected int _top = 0;
	protected Map<String,Node> _map = null;
	
	//protected Node[] _nodes = null;
	protected LinkedList<Node> _nodes = null; //***
	protected double lambda; //***
	protected double boundFrequency; //***
	
	protected int numDoc = 0;

	/**
	 * Initialise a set of counters for String tokens 
	 *
	 */
	@Override
	protected void prepareForUseImpl(TaskMonitor arg0, ObjectRepository arg1) {
		_top = 0;
		_map = new HashMap<String,Node>();
		//_nodes = new_nodes = new ArrayList<Node>();
		_nodes = new LinkedList<Node>(); //***
		this.numDoc = 0;
		this.lambda = lambdaOption.getValue();
		//this.lambda = epsilonOption.getValue()*epsilonOption.getValue()/(capacityOption.getValue() * 2 * Math.log(1/deltaOption.getValue()));
		this.boundFrequency = (1+epsilonOption.getValue())/capacityOption.getValue();
	}		
	
	public int addToken(String token, int freq, int classIndex){ 	
		return addToken(token, freq);
	}	
	
	/**
	 * Retrieve the (estimated, upper-bound) count for any token
	 *
	 * @param token 
	 * @return count (or 0 if not being counted currently, i.e. not frequent enough)
	 */
	public double getCount(String token) {
		Node node = _map.get(token);
		if (node == null) {
			return 0.0;
		} else {
			return node.getCount(this.numDoc, this.lambda);
		}
	}
		
	
	/**
	 * Get the attribute index of the token specified.
	 * @param token
	 * @return
	 */
	public int getAttIndex(String token) {
		Node node = _map.get(token);
		if (node != null) {
			return node.attrIndex;
		}
		else
			return -1;
	}
	
	/**
	 * Add a token to be counted.
	 *
	 * @param token 
	 * @return attributeIndex which is useful for constructing Instance objects
	 */

	public int addToken(String token, int freq) {
		Node node = _map.get(token);
		if (node == null) {
			if (_top < this.capacityOption.getValue()) {
				fill(token, freq);
			} else {
				newToken(token,freq);
			}	
		} else {
			//Move to the head
			_nodes.remove(node); //Remove() ***	
			_nodes.add(node); //  Add to the tail ***
			addCount(node,freq); 
			updatePosition(node,freq);
		}
	    //System.out.println("Updating "+node);
	    //if (token.equals("word1")) System.out.println(freq+" :"+getCount(token));
		return _map.get(token).attrIndex;
	}


	/**
	 * Print all nodes to System.out, useful for debugging
	 *
	 */

	public void showNodes() {
		for (Node node: _nodes) {
			if (node != null) {
				System.out.println("Count: "+getCount(node.getToken())+" "+node);
			}
		}
	}


	protected void fill(String token, int freq) {
			Node node = newNode(token,_top, freq);
			_nodes.add(node); //  Add to the tail 
			_map.put(token, node);
			updatePosition(node,freq);
			_top++;
	}


	protected void newToken(String token, int freq) {
		Node node;
		//if (_nodes.size()>0) {
			node = _nodes.remove(); //Obtain head : remove() ***
		//}
		if (node.getCount(this.numDoc, this.lambda)< this.boundFrequency) {
				node = _nodes.remove(); //Remove() ***
		}
		_nodes.add(node); //  Add to the tail ***
		_map.remove( node.token);
		_map.put(token,node);
		node.token = token;
		addCount(node,freq); //Update ?
		//node.initCount(freq, this.numDoc);
		updatePosition(node,freq);
	}


	protected void updatePosition(Node node, int freq) {
		//System.out.println("Updating "+freq+" : "+node);
	}

	
    protected boolean addCount(Node node, int freq) { //***
   		node.addCount(freq, this.lambda, this.numDoc) ;
		return false;
	}
	

    protected double numTerms = 0;
    public void addDoc(double docSize) {
		this.numDoc++;
		this.numTerms+=docSize;
		//System.out.println("====== Doc:" +this.numDoc+" lambda:"+this.lambda+"doc size:"+docSize);
		//showNodes();
		//System.out.println("====== ");
	}

	protected Node newNode(String token, int index, int freq) {
		return new Node(token,index,freq, this.numDoc);
	}

	public void remove(String token){
		Node node = _map.get(token);
		if (node != null) {
			_nodes.remove(node);
			node = null;
			_map.remove(token);
			_top--;
		}
	}

	static class Node implements Serializable, Comparable<Node> {

		private static final long serialVersionUID = 1L;
		
		int index;
		int attrIndex;
		double count;
	    int lastDoc; //***
		String token;

		Node(String token, int index, int freq, int numDoc) {
			this.index = index;
			this.attrIndex = index;
			this.token = token;
			initCount(freq, numDoc); 
			//lastDoc = numDoc; //***
		}
		
    protected boolean addCount(double freq, double lambda, int doc) {
		// We don't use doc, only on SpaceSavingAdwin
		//this.count+= freq;
		// Suppose freq = 1;
		//Speed up computing pow with a table?
		//System.out.println("Doc:"+doc+" "+this.lastDoc+" "+"exp: "+Math.pow(1.0-lambda, doc-this.lastDoc-1));
		double c = this.count;
		this.count= this.count * Math.pow(1.0-lambda, doc-this.lastDoc)+ freq*lambda; //***
		//if (this.token.equals("word1")) System.out.println(c+" ->"+this.count+" :"+freq*lambda+","+  doc+","+this.lastDoc);
		this.lastDoc = doc; //***
		
		return false;
	}
	
	
	
    protected double getCount(int doc, double lambda) {
		//return this.count;
		return this.count* Math.pow(1-lambda, doc-this.lastDoc); //*** 
	}

    protected double getCount() {
		return this.count;
	}

    protected String getToken() {
		return this.token;
	}
	
	protected void initCount(double freq, int numDoc) {
		this.count = freq;
		this.lastDoc = numDoc;
	}

		public int compareTo(Node other) { // Not used now as we don't compare nodes
			if (getCount() < other.getCount()) return -1;
			if (getCount() > other.getCount()) return 1;
			return 0;
		}


		public String toString() {
			//return "<Node " + index + " " + attrIndex + " " + token + " " + count + " >"; // "/" + min +
			return "<Node "+ index + "," + attrIndex + "," + count + "," + getCount() + ","+token+" >"; // "/" + min +
		}

	}
	
	@Override
	public void getDescription(StringBuilder arg0, int arg1) {

	}
	public double getFreqWord(String word) {
		//return getCount(word)/ (this.numTerms/(double) this.numDoc);
		return getCount(word); //Prov
	}
}
