package ru.mpei;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@XmlRootElement(name = "agentConfig")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentConfig {
    @XmlElement
    private String name;
    @XmlElement
    private boolean isInitiator;
    @XmlElement
    private String targetAgentName;

    @XmlElementWrapper(name = "neighbors")
    @XmlElement(name = "neighbor")
    private List<Neighbor> neighborList;

    public boolean isInitiator() {
        return isInitiator;
    }

    public Map<String, Integer> getNeighbors() {
        Map<String, Integer> map = new HashMap<>();
        if (neighborList != null) {
            for (Neighbor neighbor : neighborList) {
                map.put(neighbor.getName(), neighbor.getCost());
            }
        }
        return map;
    }
}

@Data
@XmlAccessorType(XmlAccessType.FIELD)
class Neighbor {
    @XmlElement
    private String name;
    @XmlElement
    private int cost;

}
