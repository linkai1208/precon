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

package org.genemania.engine.summary;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.genemania.exception.ApplicationException;

public class NullReporter implements Reporter {

	public void init(List<String> fieldNames) {
		// TODO Auto-generated method stub
		
	}

	public void init(String... fieldNames) {
		// TODO Auto-generated method stub
		
	}

	public void write(List<String> fieldValues) throws ApplicationException {
		// TODO Auto-generated method stub
		
	}

	public void write(String... fieldValues) throws ApplicationException {
		// TODO Auto-generated method stub
		
	}

	public void write(Map<String, String> record) {
		// TODO Auto-generated method stub
		
	}

	public void close() throws ApplicationException {
		// TODO Auto-generated method stub
		
	}

	public String getReportName() {
		// TODO Auto-generated method stub
		return null;
	}

}
