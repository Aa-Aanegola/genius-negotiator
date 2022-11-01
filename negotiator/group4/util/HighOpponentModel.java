package negotiator.group4.util;

import java.util.*;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;

/**
 * The HighOpponentModel provides un-conservative estimates of the utility function. It works by keeping track of the
 * offers made by the opponent and computing the value of each issue and value.
 */
public class HighOpponentModel {

    private ArrayList<HashMap<Value, Integer>> issueValueMap; // Maps every value to a numeric score
    private List<Issue> issues; // A list of issues being negotiated

    /**
     * Initializes the opponent model from the negotiation info
     * @param info information about the negotiation that this party is part of.
     */
    public void init(NegotiationInfo info){
        this.issueValueMap = new ArrayList();
        this.issues = info.getUtilitySpace().getDomain().getIssues();

        try{
            // For every issue, find its values and initialize the issueValueMap
            for(int i = 0; i < this.issues.size(); i++) {
                IssueDiscrete issue = (IssueDiscrete)this.issues.get(i);
                HashMap valueMap = new HashMap();

                for(int j = 0; j < issue.getNumberOfValues(); ++j) {
                    ValueDiscrete value = issue.getValue(j);
                    valueMap.put(value, 0);
                }
                this.issueValueMap.add(valueMap);
            }
            System.out.println(issueValueMap.toString());
        } catch (Exception e) {
            System.out.println("Opponent model failed to initialize.");
        }
    }

    /**
     * Adds the bid to the issueValueMap by breaking it down into issue-value pairs and incrementing a counter for each
     * value.
     * @param bid The opponents bid to be registered
     */
    public void registerBid(Bid bid) {
        try {
            for(int i = 0; i<issues.size(); i++){
                HashMap<Value, Integer> valueMap = issueValueMap.get(i);
                valueMap.put(bid.getValue(issues.get(i)), valueMap.get(bid.getValue(issues.get(i)))+1);
                issueValueMap.set(i, valueMap);
            }
        } catch (Exception e) {
            System.out.println("Opponent model failed to register bid");
        }
    }

    /**
     * Calculates the estimated utility of a proposed bid by calculating issue weights and the utility offered by
     * individual issue values to compute the bid utility.
     * @param bid The proposed bid whose utility (offered to the opponent) is to be calculated
     * @return The estimated utility of the supplied bid
     */
    public Double getUtility(Bid bid) {
        try {
            // We use softmax over the max frequency within each issue to decide the importance of each issue
            HashMap<Issue, Double> issueImportance = new HashMap<>();
            Double totalImportance = 0.0;
            for (int i = 0; i < issues.size(); i++) {
                Double importance = Math.exp(Collections.max(issueValueMap.get(i).values()));
                issueImportance.put(issues.get(i), importance);
                totalImportance += importance;
            }
            for (int i = 0; i < issues.size(); i++) {
                Double importance = issueImportance.get(issues.get(i)) / totalImportance;
                issueImportance.put(issues.get(i), importance);
            }

            // We use a modified softmax over the frequencies to obtain the utility for each value of every issue
            ArrayList<HashMap<Value, Double>> valueImportance = new ArrayList<>();
            for (int i = 0; i < issues.size(); i++) {
                IssueDiscrete issue = (IssueDiscrete) this.issues.get(i);
                HashMap<Value, Double> valueMap = new HashMap();

                for (int j = 0; j < issue.getNumberOfValues(); ++j) {
                    ValueDiscrete value = issue.getValue(j);
                    Double importance = Math.exp(this.issueValueMap.get(i).get(value));
                    valueMap.put(value, importance);
                    totalImportance += importance;
                }
                for (int j = 0; j < issue.getNumberOfValues(); ++j) {
                    ValueDiscrete value = issue.getValue(j);
                    Double importance = valueMap.get(value) / totalImportance;
                    valueMap.put(value, importance);
                }
                valueImportance.add(valueMap);
            }

            // Compute final utility by multiplying issue weights and value utility
            Double utility = 0.0;
            for (int i = 0; i < issues.size(); i++) {
                Value val = bid.getValue(issues.get(i));
                utility += valueImportance.get(i).get(val) * issueImportance.get(issues.get(i));
            }
            return utility;
        } catch (Exception e) {
            System.out.println("Failed to compute utility, returning standard value");
            return 0.5;
        }
    }

    /**
     * Debug function to print the issueValueMap
     */
    public void getIssueValueMap() {
        System.out.println(issueValueMap.toString());
    }
}