package vnapps.ikara.model.v35;

import java.util.Date;

import vnapps.ikara.model.v30.TextContestRule;

public class AddEventModelRequest {
    public String name;
    public String cupForDb;
    public String eventName;
    public int typeString;
    public String language;
    public Date timeShowStart;
    public Date timeShowEnd;
    public Date timeActiveStart;
    public Date timeActiveEnd;
    public TextContestRule timeRule;
    public TextContestRule mainRule;
    public TextContestRule otherRule;
    public GiftEventModelRequest gifts_1;
    public GiftEventModelRequest gifts_2;
    public GiftEventModelRequest gifts_3;
    public GiftEventModelRequest gifts_4_10;
}
