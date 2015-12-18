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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ca.mcgill.cs.stg.solitaire.cards.Card;
import ca.mcgill.cs.stg.solitaire.cards.Card.Rank;
import ca.mcgill.cs.stg.solitaire.cards.Card.Suit;
import ca.mcgill.cs.stg.solitaire.cards.Deck;

/**
 * Keeps track of the current state of the game and provides
 * a facade to it. Implements the Singleton design pattern.
 * 
 * The game state can logically be separated into four distinct 
 * conceptual elements: the deck, the discard pile, the four
 * "suit stacks" where completed suits are accumulated, and the
 * seven "working stacks" where cards can be accumulated in sequences
 * of alternating suit colors.
 * 
 * To prevent
 * this class from degenerating into a God class, responsibilities
 * are separated into package-private "manager" classes 
 * in charge of managing the state. However, these manager classes
 * are not responsible for notifying observers.
 */
public final class GameModel 
{
	private static final GameModel INSTANCE = new GameModel();
	
	private Deck aDeck = new Deck();
	private Stack<Card> aDiscard = new Stack<>();
	private SuitStackManager aSuitStacks = new SuitStackManager();
	private WorkingStackManager aWorkingStacks = new WorkingStackManager();
	private List<GameModelListener> aListeners = new ArrayList<>();
	
	/**
	 * Represents the different stacks where cards
	 * can be accumulated.
	 */
	public enum StackIndex 
	{ FIRST, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH }
	
	private GameModel()
	{
		initialize();
	}
	
	/**
	 * @return The singleton instance for this class.
	 */
	public static GameModel instance()
	{
		return INSTANCE;
	}
	
	/**
	 * Registers an observer for the state of the game model.
	 * @param pListener A listener to register.
	 */
	public void addListener(GameModelListener pListener)
	{
		aListeners.add(pListener);
	}
	
	private void notifyListeners()
	{
		for( GameModelListener listener : aListeners )
		{
			listener.gameStateChanged();
		}
	}
	
	private void initialize()
	{
		aDeck.shuffle();
		aDiscard.clear();
		aSuitStacks.initialize();
		aWorkingStacks.initialize(aDeck);
	}
	
	/**
	 * @return True if the deck has no card left in it.
	 */
	public boolean isEmptyDeck()
	{
		return aDeck.size() == 0;
	}
	
	/**
	 * @return True if the discard pile has no card in it.
	 */
	public boolean isEmptyDiscardPile()
	{
		return aDiscard.size() == 0;
	}
	
	/**
	 * Draw a card from the deck and place it on top
	 * of the discard pile.
	 * @pre !isEmptyDeck()
	 */
	public void discard()
	{
		assert !isEmptyDeck();
		aDiscard.push(aDeck.draw());
		notifyListeners();
	}
	
	/**
	 * @param pCard The card to test
	 * @param pSuit The suit to test
	 * @return True if pCard can be moved to the top of its suit stack.
	 * This is only possible if its rank is immediately superior
	 * to that of the card currently on top of the suit stack.
	 */
	public boolean canMoveToSuitStack(Card pCard, Suit pSuit )
	{
		assert pCard != null && pSuit != null;
		if( pCard.getSuit() != pSuit )
		{
			return false;
		}
		if( pCard.getRank() == Rank.ACE )
		{
			return true;
		}
		if( aSuitStacks.isEmpty(pSuit) )
		{
			return false;
		}
		return pCard.getRank().ordinal() == aSuitStacks.peek(pSuit).getRank().ordinal()+1 &&
				pCard.getSuit() == pSuit;
	}
	
	/**
	 * Moves pCard from wherever it is in a legally 
	 * movable position and adds it to its suit stack.
	 * @param pCard The card to move.
	 */
	public void moveToSuitStack(Card pCard)
	{
		assert canMoveToSuitStack(pCard, pCard.getSuit());
		if( !aDiscard.isEmpty() && aDiscard.peek() == pCard )
		{
			aDiscard.pop();
		}
		else if( aWorkingStacks.isInStacks(pCard))
		{
			aWorkingStacks.popTopCard(pCard);
		}
		aSuitStacks.push(pCard);
		notifyListeners();
	}
	
	
	public boolean hasTopPileCard(Suit pSuit)
	{
		return !aSuitStacks.isEmpty(pSuit);
	}
	
	public Card getTopPileCard(Suit pSuit)
	{
		return aSuitStacks.peek(pSuit);
	}
	
	
	
	public boolean canDropOnStack(Card pCard, StackIndex pIndex )
	{
		return aWorkingStacks.canDropOnStack(pCard, pIndex); 
	}
	
	/**
	 * Get the sequence consisting of pCard and all 
	 * the other cards below it.
	 * @param pCard
	 * @param pIndex
	 * @return A non-empty sequence of cards.
	 */
	public Card[] getSequence(Card pCard, StackIndex pIndex)
	{
		return aWorkingStacks.getSequence(pCard, pIndex);
	}
	
	public void dropToStack(Card[] pCards, StackIndex pIndex)
	{
		// If there is only one card, move it
		if( pCards.length == 1 )
		{
			moveOneCardToWorkingStack( pCards[0], pIndex);
		}
		else // The source is a working stack, unwind
		{
			Stack<Card> temp = new Stack<>();
			for( int i = pCards.length-1; i >=0; i-- )
			{
				aWorkingStacks.popTopCard(pCards[i]);
				temp.push(pCards[i]);
			}
			while( !temp.isEmpty() )
			{
				aWorkingStacks.push(temp.pop(), pIndex);
			}
		}
		
		notifyListeners();
	}
	
	private void moveOneCardToWorkingStack( Card pCard, StackIndex pIndex)
	{
		if( !aDiscard.isEmpty() && aDiscard.peek() == pCard )
		{
			aDiscard.pop();
		}
		else if( !aSuitStacks.isEmpty(pCard.getSuit()) && aSuitStacks.peek(pCard.getSuit()) == pCard )
		{
			aSuitStacks.pop(pCard.getSuit());
		}
		else if( aWorkingStacks.isInStacks(pCard))
		{
			aWorkingStacks.popTopCard(pCard);
		}
		aWorkingStacks.push(pCard, pIndex);
	}
	
	public CardView[] getStackAt(StackIndex pIndex)
	{
		return aWorkingStacks.getStack(pIndex); 
	}
	
	
	public Card getDiscardPileTop()
	{
		assert aDiscard.size() != 0;
		return aDiscard.peek();
	}
	
	
}
