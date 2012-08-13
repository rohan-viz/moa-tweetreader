/*
 *    FilterTfIdf.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Kenneth Gibson (kjjg1@waikato.ac.nz)
 *
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
package moa.streams.twitter;

import moa.core.InstancesHeader;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.SparseInstance;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * The class for filtering a Tweet using Tf-Idf weightings
 * 
 * @author Kenneth Gibson (kjjg1@waikato.ac.nz)
 *
 */
public class FilterTfIdf {

    private ArrayList<String> stringList = new ArrayList<String>();

    protected Sketch frequentItemMiner;

    protected double numOfDocs = 0;	//Number of documents in total.

    public FilterTfIdf(Sketch sketch) {
        this.frequentItemMiner = sketch;
    }

    /**
     * Takes a String, filters it, and calculates the tf-idf values
     * of each token in the string.
     *
     * @param s - the String to be filtered
     * @return the filtered Instance object
     */
    public Instance filter(String s, InstancesHeader header) {

        this.numOfDocs++;	//New document, count it.
        String[] split = s.split(",");	//The message and type are separated by a comma.
        String message = split[0];
        String type = split[1];
        //System.out.println(type);
        //String message = s;
        //String type = "H";


        message = message.replaceAll("'", "");
        String[] tokens = message.split(" ");	//Get the individual tokens.
        double docSize = tokens.length; 		//Number of tokens in the document.

        //The tokens and frequency in this specific document.
        Map<String, Integer> tokensInDoc = new HashMap<String, Integer>();

        for (String token : tokens) {
            //For each token in the document
            if (!token.equals(" ") && !token.equals("")) {
                Integer freq = tokensInDoc.get(token.toLowerCase()); //Compute freq for each token
                tokensInDoc.put(token.toLowerCase(), (freq == null) ? 1 : freq + 1);
            }
        }

        for (Map.Entry<String, Integer> e : tokensInDoc.entrySet()) { //For each token in the document
            //	System.out.println(e.getKey()+" "+e.getValue());
            int oldAttIndex = frequentItemMiner.getAttIndex(e.getKey());
            frequentItemMiner.addToken(e.getKey(), e.getValue(), type.equals("S") ? 1 : 0);

            int newAttIndex = frequentItemMiner.getAttIndex(e.getKey());
            if (oldAttIndex == -1) {
                // Add a new attribute since it was not there
                if (newAttIndex + 1 > header.numAttributes() - 1) {
                    // Add a new attribute
                    //System.out.println("Add "+e.getKey()+" "+e.getValue());
                    Attribute newAtt = new Attribute(e.getKey());
                    header.insertAttributeAt(newAtt, newAttIndex + 1);

                } else {
                    // Change the name of the attribute
                    //System.out.println((newAttIndex +1 )+ "> "+(header.numAttributes() -1)+ "Change "+e.getKey()+" "+e.getValue());
                    header.renameAttribute(newAttIndex + 1, e.getKey());
                }
            }
        }

        frequentItemMiner.addDoc(docSize);
        // Create an sparse instance
        int numTokens = (int) tokensInDoc.size();
        double[] attValues = new double[numTokens];
        int[] indices = new int[numTokens];

        int tokenCounter = 0;
        for (Map.Entry<String, Integer> e : tokensInDoc.entrySet()) { //For each token in the document
            String token = e.getKey();
            double numInDoc = e.getValue(); 				//Number of occurrences of a token in the specific document.
            double docFreq = frequentItemMiner.getCount(token);		//Number of documents that the token appears in.
            double tf = numInDoc / docSize; 							//Term frequency.
            double idf = Math.log10(this.numOfDocs / (docFreq + 1)); 	//Inverse document frequency.
            int attIndex = frequentItemMiner.getAttIndex(token) + 1;
            indices[tokenCounter] = attIndex;
            attValues[tokenCounter] = (tf * idf);								//tf*idf
            tokenCounter++;
            //System.out.println((tokenCounter-1)+ "tf =" + tf+" idf =" +idf+ "Add "+ attIndex+ " "+e.getKey()+" "+e.getValue());
        }

        Instance inst = new SparseInstance(1.0, attValues, indices, header.numAttributes());
        inst.setDataset(header);
        //System.out.println("B"+type);
        if (type.equals("S") || type.equals("H")) {
            //System.out.println(type.equals("S") ? "S" : "H");
            inst.setClassValue(type.equals("S") ? "S" : "H");
        } else {
            inst.setClassMissing();
        }
        return inst;

    }

    public void printSketch() {
        this.frequentItemMiner.showNodes();
    }

    public double getFreqWord(String word) {
        return frequentItemMiner.getFreqWord(word);
    }

}
