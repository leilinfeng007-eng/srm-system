package com.srm.platform.navigation.infrastructure.persistence;

import com.srm.platform.navigation.MenuNode;
import com.srm.platform.navigation.NavigationRepository;
import com.srm.platform.navigation.infrastructure.persistence.dto.NavigationMenuRow;
import com.srm.platform.navigation.infrastructure.persistence.mapper.NavigationMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisNavigationRepository implements NavigationRepository {

    private final NavigationMapper mapper;

    public MybatisNavigationRepository(NavigationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<MenuNode> findGrantedTree(long userId) {
        List<NavigationMenuRow> rows = mapper.selectGrantedRows(userId);
        Map<Long, NodeBuilder> nodes = new LinkedHashMap<>();
        for (NavigationMenuRow row : rows) {
            nodes.put(row.getId(), new NodeBuilder(row));
        }
        List<NodeBuilder> roots = new ArrayList<>();
        for (NodeBuilder node : nodes.values()) {
            if (node.row.getParentId() == null) {
                roots.add(node);
            } else {
                NodeBuilder parent = nodes.get(node.row.getParentId());
                if (parent != null) {
                    parent.children.add(node);
                }
            }
        }
        return roots.stream().map(NodeBuilder::build).toList();
    }

    private static final class NodeBuilder {
        private final NavigationMenuRow row;
        private final List<NodeBuilder> children = new ArrayList<>();

        private NodeBuilder(NavigationMenuRow row) {
            this.row = row;
        }

        private MenuNode build() {
            return new MenuNode(
                    row.getMenuCode(),
                    row.getLabel(),
                    row.getRoute(),
                    row.getComponentKey(),
                    row.getPermission(),
                    row.getSortOrder(),
                    children.stream().map(NodeBuilder::build).toList());
        }
    }
}
