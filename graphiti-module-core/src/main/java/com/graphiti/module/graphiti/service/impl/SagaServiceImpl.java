package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.SagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaServiceImpl implements SagaService {

    private final Driver neo4jDriver;

    @Override
    public Map<String, Object> createSaga(String graphId, String name, List<String> episodeUuids) {
        String sagaUuid = UUID.randomUUID().toString().replace("-", "");

        String createSagaCypher =
            "CREATE (s:Saga {group_id: $group_id, uuid: $uuid, name: $name})";

        try (Session session = neo4jDriver.session()) {
            session.run(createSagaCypher, Values.parameters(
                "group_id", graphId,
                "uuid", sagaUuid,
                "name", name
            ));
        }

        for (int i = 0; i < episodeUuids.size(); i++) {
            String episodeUuid = episodeUuids.get(i);
            String linkCypher =
                "MATCH (s:Saga {group_id: $group_id, uuid: $saga_uuid}) " +
                "MATCH (e:Episode {group_id: $group_id, uuid: $episode_uuid}) " +
                "CREATE (s)-[:HAS_EPISODE {order: $order}]->(e)";

            try (Session session = neo4jDriver.session()) {
                session.run(linkCypher, Values.parameters(
                    "group_id", graphId,
                    "saga_uuid", sagaUuid,
                    "episode_uuid", episodeUuid,
                    "order", i
                ));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sagaUuid", sagaUuid);
        result.put("episodeCount", episodeUuids.size());
        return result;
    }

    @Override
    public List<Map<String, Object>> listSagas(String graphId) {
        String cypher =
            "MATCH (s:Saga {group_id: $group_id}) " +
            "RETURN s.uuid as uuid, s.name as name " +
            "ORDER BY s.name";

        List<Map<String, Object>> sagas = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters("group_id", graphId));
            while (result.hasNext()) {
                var record = result.next();
                Map<String, Object> saga = new HashMap<>();
                saga.put("uuid", record.get("uuid").asString());
                saga.put("name", record.get("name").asString());
                sagas.add(saga);
            }
        }
        return sagas;
    }

    @Override
    public List<Map<String, Object>> getSagaEpisodes(String sagaUuid) {
        String cypher =
            "MATCH (s:Saga {uuid: $saga_uuid})-[h:HAS_EPISODE]->(e:Episode) " +
            "RETURN e.uuid as uuid, e.name as name, h.order as order " +
            "ORDER BY h.order";

        List<Map<String, Object>> episodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters("saga_uuid", sagaUuid));
            while (result.hasNext()) {
                var record = result.next();
                Map<String, Object> episode = new HashMap<>();
                episode.put("uuid", record.get("uuid").asString());
                episode.put("name", record.get("name").asString());
                episode.put("order", record.get("order").asInt());
                episodes.add(episode);
            }
        }
        return episodes;
    }
}
