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
 * SketchDiscriminative.java
 * Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.streams.twitter;

import moa.options.ClassOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.AbstractOptionHandler;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the so called SpaceSaving algorithm
 * for counting frequent items, actually String tokens in this version
 * using exponential decays. Cormode et al.
 *
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * 
 * Modified for TfIdf
 *
 */
public class SketchDiscriminative extends AbstractOptionHandler implements Sketch {

	private static final long serialVersionUID = 1L;
	
	//public IntOption capacityOption = new IntOption("capacity", 'k', "Number of attributes to use", 1000);
	
	//public FloatOption epsilonOption = new FloatOption("epsilon", 'e', "Epsilon to use", .01);
	
	//public FloatOption deltaOption = new FloatOption("delta", 'd', "Delta to use", .01);
	
	//public FloatOption lambdaOption = new FloatOption("lambda", 'l', "Lambda to use", .01);

	public ClassOption sketchOption = new ClassOption("sketch", 's',
	 		// "Sketch algorithm to use.", Sketch.class, "SpaceSaving");
             "Sketch algorithm to use.", Sketch.class, "LRUSketch");//"SpaceSaving");
             
    protected Sketch[] sketch;
    
    protected Attributes attributes;

	static class Attributes implements Serializable {

		protected Map<String, Integer> indexAttributes;
		protected List<Integer> emptyAttributes;
		
		public Attributes(){
			this.indexAttributes = new HashMap<String, Integer>();
			this.emptyAttributes = new ArrayList<Integer>();
			for (int i = 0; i<1000;i++){
				this.emptyAttributes.add(i);
			}
		}
		
		public void remove(String token){
			Integer index = indexAttributes.get(token);
			if (index != null) {
				this.emptyAttributes.add(index);
				this.indexAttributes.remove(token);
			} 
		}
		
		public void add(String token){
			Integer index = indexAttributes.get(token);
			if (index == null) {
				if (this.emptyAttributes.size() >0){
					index = this.emptyAttributes.remove(0);
				}
				if (index != null) {
					this.indexAttributes.put(token,index);
				}
			} 
		}
		
		
		public int getAttIndex(String token){
			Integer index = indexAttributes.get(token);
			if (index == null) {
				index = -1;
			}
			return index;
		}
		
	}
	
	protected int numClasses = 2;
	
	@Override
	protected void prepareForUseImpl(TaskMonitor arg0, ObjectRepository arg1) {
		Sketch base = (Sketch) getPreparedClassOption(this.sketchOption);
		sketch = new Sketch[this.numClasses+1];
		for (int i=0;i<this.numClasses+1;i++) {
			sketch[i] = (Sketch) ((AbstractOptionHandler) base).copy();
		}
		this.attributes = new Attributes();
	}	

	public int addToken(String token, int freq){
		return addToken(token,freq,0);
	}
	public int addToken(String token, int freq, int classIndex){ //insert()
		//Update sketch
		if (getAttIndex(token) == -1) {
			//New attribute
			attributes.add(token);
		}
		if (sketch[0].getCount(token) == 0) { //If it is not on the stop list
			sketch[classIndex+1].addToken(token,freq);
			//Check for evidence
			if (Math.abs(sketch[1].getFreqWord(token)-sketch[2].getFreqWord(token))<.01) {
				//Not discriminatory
				sketch[1].remove(token);
				sketch[2].remove(token);
				//add to stoplist
				sketch[0].addToken(token,freq);
				attributes.remove(token);
			} 
		} else {
			sketch[0].addToken(token,freq *(1- classIndex*2)); // Add or substract freq
			//Check for evidence
			if (Math.abs(sketch[0].getFreqWord(token))>.1) {	
				sketch[sketch[0].getFreqWord(token) > 0 ? 1 : 2].addToken(token,freq);
				sketch[0].remove(token);
				attributes.add(token);
			}
		}		
		return getAttIndex(token);
	}

	public void showNodes() {
		for (int i=0;i<this.numClasses+1;i++) {
			sketch[i].showNodes();
		}
	}

	public void addDoc(double docSize){
		for (int i=0;i<this.numClasses+1;i++) {
			sketch[i].addDoc(docSize);
		}
	}
	
	public double getCount(String token){
		double ret= 0;
		for (int i=0;i<this.numClasses+1 && ret == 0.0;i++) {
			ret+= sketch[i].getCount(token);
		}
		return ret;
		//Compute mean of counts for tf-idf
	}
	
	public int getAttIndex(String token){
		return this.attributes.getAttIndex(token);
	}

	public double getFreqWord(String word) {
		//return getCount(word)/ (this.numTerms/(double) this.numDoc);
		return getCount(word); //Prov
	}
	
	@Override
	public void getDescription(StringBuilder arg0, int arg1) {

	}
	
	public void remove(String token){
	}

}
