package ru.mpei;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;

import java.util.*;

public class NodeAgent extends Agent {
    private boolean isInitiator;
    private String targetAgentName;
    private Map<String, Integer> neighbors = new HashMap<>();

    private Map<String, Integer> minCosts = new HashMap<>();

    private Map<String, String> previousNodes = new HashMap<>();

    private Map<String, Integer> minCostToTarget = new HashMap<>();

    private Map<Integer, Boolean> pathFoundMap = new HashMap<>();


    private static int requestIdCounter = 0;
    private int requestId;

    @Override
    protected void setup() {
        loadConfiguration();

        if (isInitiator) {
            requestId = ++requestIdCounter;
            minCosts.put(requestId + "_" + getLocalName(), 0);
            previousNodes.put(requestId + "_" + getLocalName(), null);
            addBehaviour(new InitiateSearchBehaviour());
        }

        addBehaviour(new MessageReceiverBehaviour());
    }

    private boolean loadConfiguration() {
        String path = "src/main/resources/" + getLocalName() + ".xml";
        Optional<AgentConfig> optionalConfig = XmlSerialization.deserialize(path, AgentConfig.class);

        if (optionalConfig.isPresent()) {
            AgentConfig config = optionalConfig.get();

            this.isInitiator = config.isInitiator();
            this.targetAgentName = config.getTargetAgentName();
            this.neighbors = config.getNeighbors();

            return true;
        } else {
            return false;
        }
    }

    // Поведение инициации процесса поиска
    private class InitiateSearchBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                int edgeCost = entry.getValue();

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setConversationId("search");
                // Содержимое: requestId; инициатор; targetAgentName; стоимость; путь
                msg.setContent(requestId + ";" + getLocalName() + ";" + targetAgentName + ";" + edgeCost + ";" + getLocalName());
                msg.addReceiver(new AID(neighbor, false));
                send(msg);
                System.out.println(getLocalName() + ": Отправил запрос агенту " + neighbor + " с начальной стоимостью " + edgeCost);
            }
        }
    }

    // Поведение обработки входящих сообщений
    private class MessageReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();

            if (msg != null) {
                if (msg.getConversationId().equals("search")) {
                    addBehaviour(new HandleSearchRequestBehaviour(msg));
                } else if (msg.getConversationId().equals("response")) {
                    addBehaviour(new HandleResponseBehaviour(msg));
                }
            } else {
                block();
            }
        }
    }

    // Поведение обработки запроса поиска
    private class HandleSearchRequestBehaviour extends OneShotBehaviour {
        private ACLMessage msg;

        public HandleSearchRequestBehaviour(ACLMessage msg) {
            this.msg = msg;
        }

        @Override
        public void action() {
            String content = msg.getContent();
            String[] parts = content.split(";");
            int receivedRequestId = Integer.parseInt(parts[0]);
            String initiator = parts[1];
            String messageTargetAgentName = parts[2];
            int cost = Integer.parseInt(parts[3]);
            String path = parts[4];
            if (pathFoundMap.getOrDefault(receivedRequestId, false)) {
                return;
            }

//            System.out.println(getLocalName() + " получил запрос поиска от " + msg.getSender().getLocalName() +
//                    ". Инициатор: " + initiator + ", Целевой агент: " + messageTargetAgentName+ ", Стоимость: " + cost + ", Путь: " + path);

            String key = receivedRequestId + "_" + getLocalName();
            if (minCosts.containsKey(key) && minCosts.get(key) <= cost) {
                // Если уже есть более дешевый путь, игнорируем сообщение
                return;
            }

            // Обновляем минимальную стоимость и предыдущий узел
            minCosts.put(key, cost);
            previousNodes.put(key, msg.getSender().getLocalName());

            if (getLocalName().equals(messageTargetAgentName)) {
                int currentMinCost = minCostToTarget.getOrDefault(key, Integer.MAX_VALUE);
                if (cost < currentMinCost) {
                    minCostToTarget.put(key, cost);
                    ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                    reply.setConversationId("response");
                    reply.addReceiver(new AID(msg.getSender().getLocalName(), false));
                    reply.setContent(receivedRequestId + ";" + getLocalName() + ";" + cost + ";" + path + "->" + getLocalName());
                    send(reply);
                    System.out.println(getLocalName() + ": Достигнут целевой агент с новой минимальной стоимостью. Отправляем ответ обратно.");
                } else {
                    System.out.println(getLocalName() + ": Достигнут целевой агент, но стоимость не минимальна. Сообщение игнорируется.");
                }
            } else {
                // Пересылаем запрос соседям
                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    String neighbor = entry.getKey();
                    int edgeCost = entry.getValue();

                    if (!neighbor.equals(msg.getSender().getLocalName())) {
                        ACLMessage forwardMsg = new ACLMessage(ACLMessage.REQUEST);
                        forwardMsg.setConversationId("search");
                        forwardMsg.addReceiver(new AID(neighbor, false));
                        forwardMsg.setContent(receivedRequestId + ";" + initiator + ";" + messageTargetAgentName + ";" + (cost + edgeCost) + ";" + path + "->" + getLocalName());
                        send(forwardMsg);
                    }
                }
            }
        }
    }

        // Поведение обратной передачи сообщения
        private class HandleResponseBehaviour extends OneShotBehaviour {
            private ACLMessage msg;

            public HandleResponseBehaviour(ACLMessage msg) {
                this.msg = msg;
            }

            @Override
            public void action() {
                String content = msg.getContent();
                String[] parts = content.split(";");
                int receivedRequestId = Integer.parseInt(parts[0]);
                String node = parts[1];
                int cost = Integer.parseInt(parts[2]);
                String path = parts[3];
                if (pathFoundMap.getOrDefault(receivedRequestId, false)) {
                    return;
                }

//                System.out.println(getLocalName() + " получил ответ от " + msg.getSender().getLocalName() +
//                        ". Узел: " + node + ", Стоимость: " + cost + ", Путь: " + path);

                String key = receivedRequestId + "_" + getLocalName();

                if (isInitiator) {
                    if (!pathFoundMap.getOrDefault(receivedRequestId, false)) {
                        pathFoundMap.put(receivedRequestId, true);
                        System.out.println(getLocalName() + ": Кратчайший путь найден: " + path + ", стоимость: " + cost);
                    } else {
                        System.out.println(getLocalName() + ": Путь уже найден ранее. Сообщение игнорируется.");
                    }
                } else {
                    // Передаем ответ предыдущему узлу
                    String previousNode = previousNodes.get(key);
                    if (previousNode != null) {
                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                        reply.setConversationId("response");
                        reply.addReceiver(new AID(previousNode, false));
                        reply.setContent(receivedRequestId + ";" + node + ";" + cost + ";" + path);
                        send(reply);
                    }
                }
            }
        }
    }
