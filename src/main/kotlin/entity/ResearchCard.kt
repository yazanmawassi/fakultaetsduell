package entity


/**
* Represents a specific research card ("Forschungskarte") within the game.
*
* Research cards are the core elements of the game's economy and scoring system. They are
* drawn as ideas into a player's hand, placed onto research activity stacks on the table,
* and permanently secured into the publications repository through successful publication actions.
*
* @property suit The card category or suit color (e.g., HEARTS, SPADES) defining which research topic this card belongs to.
* @property value The specific card rank or content value which can influence interactions or indicate the card's rank.
*/
class ResearchCard(
    val suit: CardSuit,
    val value: CardValue
)