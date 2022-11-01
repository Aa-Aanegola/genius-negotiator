package negotiator.group4;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.misc.Range;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import negotiator.group4.util.HighOpponentModel;

/**
 * Group4_Agent is a negotiation agent that implements a boulware curve based concession protocol and offering strategy and an
 * opponent model that identifies bias towards values for all issues.
 */
public class Group4_Agent extends AbstractNegotiationParty {
    private Bid lastReceivedBid = null; // Stores the last bid received from the opponent
    private SortedOutcomeSpace bids; // A list of all bids sorted based on utility
    private List offerList; // An offer list in decreasing order of utility
    private int index; // The index corresponding to the previous offer from the offer list
    private double prevOpponentUtility; // The utility offered to the agent by the opponent in @lastReceivedBid
    private double delta; // The range that the negotiation agent is akin to accepting offers is [cur-delta, cur+delta]
    private double lowerBound; // The lowest utility that the agent will accept
    private HighOpponentModel opponentModel; // The opponent model that the agent uses to select offers
    private List<BidDetails> sortedBids;


    /**
     * Initializes the agent and sets its parameters
     * @param info information about the negotiation that this party is part of.
     */
    @Override
    public void init(NegotiationInfo info) {

        super.init(info);
        System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
        System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());
        System.out.println("Utility space" + getUtilitySpace());

        this.bids = new SortedOutcomeSpace(getUtilitySpace());
        sortedBids = bids.getOrderedList();

        // Not being used right now, will be used to calculate time step specific optimal bid
        this.delta = 0.05;
        this.lowerBound = 0.5;

        this.opponentModel = new HighOpponentModel();
        this.opponentModel.init(info);
    }

    /**
     * Selects an action [Accept, Offer, Decline] based on the opponents offer, time remaining and the opponent model.
     * Calculates a desired utility and if the opponent offers more utility accepts. If not, selects the offer with the
     * highest calculated utility for the opponent from the opponent model.
     * @param validActions List of all actions possible.
     * @return One of [Accept, Offer] based on whether the agent accepts the offer or proposes an alternative.
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {
        // Use the time and the deadline to compute a desired utility
        double timeLeft = super.timeline.getTotalTime() - super.timeline.getCurrentTime();
        double currentUtility = Math.exp(Math.sqrt(Math.cbrt(timeLeft/super.timeline.getTotalTime())))/Math.exp(1.0);
        System.out.println("Current desired utility " + currentUtility);

        // Compute a starting bid based on the desired utility
        Bid offer = bids.getBidNearUtility(currentUtility).getBid();

        System.out.println("Current time" + (super.timeline.getTotalTime() - super.timeline.getCurrentTime()));

        // Propose the offer, or if the opponent offered us something better accept
        if(lastReceivedBid == null || !validActions.contains(Accept.class)){
            System.out.println("No received bid, offering " + offer + "with utility" + getUtility(offer));
            return new Offer(getPartyId(), offer);
        }
        if(prevOpponentUtility > Math.min(currentUtility, 0.85) ||
                (prevOpponentUtility > this.lowerBound) && rand.nextDouble() > 0.75){
            return new Accept(getPartyId(), lastReceivedBid);
        }
        // Propose a counter-offer we select the bid within a permissible range that is the best for our opponent
        Range utilityRange = new Range(Math.max(currentUtility-this.delta, this.lowerBound),
                Math.min(currentUtility+this.delta, 1.0));
        List<BidDetails> bestBidDetails = bids.getBidsinRange(utilityRange);

        // From the list of acceptable offers, select the one that offers the opponent the most utility
        Double bestUtility = 0.0;
        Bid bestBid = null;
        for(int i = 0; i<bestBidDetails.size(); i++){
            Bid bid = bestBidDetails.get(i).getBid();
            Double opponentUtility = opponentModel.getUtility(bid);
            if(opponentUtility > bestUtility) {
                bestUtility = opponentUtility;
                bestBid = bid;
            }
        }
        if(bestBid != null)
            offer = bestBid;

        // Randomly change the bid to the best possible bid under the assumption that the opponent is conceding
        if(rand.nextDouble() > 0.75 && timeLeft < super.timeline.getTotalTime()/3){
            int pos = rand.nextInt(Math.min(5, sortedBids.size()));
            offer = sortedBids.get(pos).getBid();
            System.out.println("Randomly replaced with" + pos + " best bid" + getUtility(offer));
        }

        System.out.println("Not content, offering " + offer + "with utility" + getUtility(offer)
                + "and opponent utility " + opponentModel.getUtility(offer));
        return new Offer(getPartyId(), offer);
    }

    /**
     * Receives the opponents previous action and sets lastReceivedBid, prevOpponentUtility and updates the opponent
     * model.
     * @param sender
     *            The initiator of the action.This is either the AgentID, or
     *            null if the sender is not an agent (e.g., the protocol).
     * @param action
     *            The action performed
     */
    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
            prevOpponentUtility = getUtility(lastReceivedBid);
            opponentModel.registerBid(lastReceivedBid);
        }
    }

    /**
     * Returns the description of the agent
     * @return The time stamp of last build and a description
     */
    @Override
    public String getDescription() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return "[" + formatter.format(date) + "] A hardy pseudo-boulware agent with an opponent model - " +
                "6th power exponential curve & softmax approximations";
    }

}