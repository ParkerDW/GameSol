/*******************************************************************************
 * Solitaire
 *
 * Copyright (C) 2016 by Martin P. Robillard
 *
 * See: https://github.com/prmr/Solitaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package ca.mcgill.cs.stg.solitaire.model;

import java.lang.reflect.Method;

import ca.mcgill.cs.stg.solitaire.cards.Deck;

/**
 * Models a deck of 52 cards that is not 
 * shuffled so that the cards drawn can be predicted,
 * in order of suit then rank.
 */
public class TestDeck extends Deck
{
	/**
	 * Only resets the deck without shuffling it.
	 */
	@Override
	public void shuffle()
	{
		try
		{
			Method reset = Deck.class.getDeclaredMethod("reset");
			reset.setAccessible(true);
			reset.invoke(this, new Object[0]);
		}
		catch( Exception pException )
		{
			pException.printStackTrace();
		}
	}
}
