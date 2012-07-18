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
package org.genemania.service;

import java.util.Collection;

import org.genemania.domain.InteractionNetwork;
import org.genemania.exception.DataStoreException;

/**
 * Provides access to network domain objects
 */
public interface NetworkService {

	/**
	 * Gets the default networks for an organism
	 * 
	 * @param id
	 *            The organism ID
	 * @param sessionId
	 *            The session ID
	 * @return The set of default networks
	 */
	public Collection<InteractionNetwork> findDefaultNetworksForOrganism(
			Long id, String sessionId) throws DataStoreException;

	/**
	 * Gets the default networks for an organism
	 * 
	 * @param id
	 *            The organism ID
	 * @param sessionId
	 *            The session ID
	 * @param includeUserNetworks
	 *            Whether to return user networks in the return value
	 * @return The set of default networks
	 */
	public Collection<InteractionNetwork> findDefaultNetworksForOrganism(
			Long id, String sessionId, boolean includeUserNetworks)
			throws DataStoreException;

	/**
	 * Gets a network by ID
	 * 
	 * @param id
	 *            The network ID
	 * @return The network
	 * @throws DataStoreException
	 *             on database error
	 */
	public InteractionNetwork findNetwork(long id) throws DataStoreException;

	/**
	 * Gets the networks specified by ID or the default networks if no IDs
	 * specified
	 * 
	 * @param organismId
	 *            The organism ID for the networks
	 * @param networkIds
	 *            The IDs of the networks
	 * @return The set of networks
	 * @throws DataStoreException
	 *             on database error
	 */
	public Collection<InteractionNetwork> getNetworks(Integer organismId,
			Long[] networkIds, String sessionId) throws DataStoreException;

	/**
	 * Gets the networks specified by ID or the default networks if no IDs
	 * specified
	 * 
	 * @param organismId
	 *            The organism ID for the networks
	 * @param networkIds
	 *            The IDs of the networks
	 * @param includeUserNetworks
	 *            Whether to include user networks in the return value
	 * @return The set of networks
	 * @throws DataStoreException
	 *             on database error
	 */
	public Collection<InteractionNetwork> getNetworks(Integer organismId,
			Long[] networkIds, String sessionId, boolean includeUserNetworks)
			throws DataStoreException;

	/**
	 * Adds a network to the engine
	 * 
	 * @param organismId
	 *            The organism for the network
	 * @param sessionId
	 *            The user's sesison ID
	 * @param network
	 *            The network
	 */
	public void addUserNetwork(long organismId, String sessionId,
			InteractionNetwork network);

	/**
	 * Deleets a user network from the engine
	 * 
	 * @param organismId
	 *            The organism ID for the network
	 * @param networkId
	 *            The ID of the network
	 * @param sessionId
	 *            The user's session ID
	 * @throws DataStoreException
	 *             if the network can't be deleted (e.g. it doesn't exist)
	 */
	public void deleteUserNetwork(long organismId, long networkId,
			String sessionId) throws DataStoreException;

	/**
	 * Get the user networks uploaded to the engine
	 * 
	 * @param organismId
	 *            The organism of the networks
	 * @param sessionId
	 *            The user's session ID
	 * @return The networks
	 */
	public Collection<InteractionNetwork> getUserNetworks(long organismId,
			String sessionId);
}
