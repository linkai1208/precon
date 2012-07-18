/**
 * This file is part of GeneMANIA.
 * Copyright (C) 2010 University of Toronto.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.genemania.engine.validation;

import junit.framework.TestCase;


public class AucPrTest extends TestCase{

	public void test1(){
		boolean [] classes = {false, true, false, true, true, true, false, true, false, false};
		double [] scores = {1, 10, 9, 8, 7, 6, 5, 4, 3, 2};
		
		// correctResults obtained from Sara's calcPR2 Matlab code
		// correctResults[i] = precision at (i*10)% recall
		double[] correctResults = {1, 1, 0.6667, 0.6667, 0.75, 0.75, 0.8, 0.8, 0.7143, 0.7143};
		// get the area under the pr curve from average precision
		double total = 0;
		for ( int i = 0; i < 10; i++ ){
			total += correctResults[i];
		}
		
		AucPr measure = new AucPr("AucPR");
		double result = measure.computeResult(classes, scores);
		assertEquals(total/correctResults.length, result, 0.0001);
	}
	
	public void test2(){
		boolean [] classes = {false, true, false, true, false, true, true, true, false, false};
		double [] scores = {1, 10, 9, 8, 7, 6, 5, 4, 3, 2 };
		
		// correctResults obtained from Sara's calcPR2 Matlab code
		// correctResults[i] = precision at (i*10)% recall
		double[] correctResults = {1, 1, 0.6667, 0.6667, 0.6, 0.6, 0.6667, 0.6667, 0.7143, 0.7143};
		// get the area under the pr curve from average precision
		double total = 0;
		for ( int i = 0; i < 10; i++ ){
			total += correctResults[i];
		}
		
		AucPr measure = new AucPr("AucPR");
		double result = measure.computeResult(classes, scores);
		assertEquals(total/correctResults.length, result, 0.0001);
	}
	
	public void test3(){
		boolean [] classes = {false, false, false, false, false, false, false, false, false, false};
		double [] scores = {1, 10, 9, 8, 7, 6, 5, 4, 3, 2 };
		
		AucPr measure = new AucPr("AucPR");
		double result = measure.computeResult(classes, scores);
		assertEquals(0, result, 0.0001);
	}
}
