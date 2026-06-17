<template>
  <div class="graph-ide-container">
    <!-- Header -->
    <header class="ide-header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 32 32" fill="none" width="28" height="28">
            <circle cx="16" cy="16" r="14" stroke="url(#logoGradient)" stroke-width="2"/>
            <circle cx="10" cy="12" r="3" fill="#58a6ff"/>
            <circle cx="22" cy="12" r="3" fill="#a371f7"/>
            <circle cx="16" cy="22" r="3" fill="#3fb950"/>
            <defs>
              <linearGradient id="logoGradient" x1="0" y1="0" x2="32" y2="32">
                <stop offset="0%" stop-color="#58a6ff"/>
                <stop offset="100%" stop-color="#a371f7"/>
              </linearGradient>
            </defs>
          </svg>
          <span class="logo-text">OntoGraph</span>
        </div>
        
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="$router.push('/graph/ide')">{{ t('graphIde.breadcrumb') }}</span>
          <span class="breadcrumb-sep">/</span>
          <a-select
            v-if="!graphId"
            v-model:value="selectedGraphId"
            :placeholder="t('graphIde.selectGraphPlaceholder')"
            style="width: 160px; margin-left: 8px;"
            :loading="loadingGraphs"
            @change="handleGraphChange"
          >
            <a-select-option v-for="g in graphList" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
          <span v-else class="breadcrumb-current">{{ graphData?.name || t('graphIde.loading') }}</span>
        </div>
      </div>

      <div class="header-actions">
        <a-button class="action-btn" @click="handleSync">
          <template #icon><SyncOutlined :spin="syncing" /></template>
          {{ t('graphIde.sync') }}
        </a-button>
      </div>
    </header>

    <!-- Main Content -->
    <div class="ide-main">
      <!-- Sidebar -->
      <aside class="ide-sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-header">
          <template v-if="!sidebarCollapsed">
            <div class="sidebar-tabs">
              <button
                class="sidebar-tab"
                :class="{ active: sidebarTab === 'ontology' }"
                @click="sidebarTab = 'ontology'"
              >
                {{ t('graphIde.sidebarOntology') }}
              </button>
              <button
                class="sidebar-tab"
                :class="{ active: sidebarTab === 'episodes' }"
                @click="sidebarTab = 'episodes'"
              >
                {{ t('graphIde.sidebarEpisodes') }}
              </button>
              <button
                class="sidebar-tab"
                :class="{ active: sidebarTab === 'communities' }"
                @click="sidebarTab = 'communities'"
              >
                {{ t('graphIde.sidebarCommunities') }}
              </button>
            </div>
          </template>
          <div class="sidebar-collapse-btns">
            <a-tooltip :title="sidebarCollapsed ? t('graphIde.expandSidebar') : t('graphIde.collapseSidebar')">
              <a-button type="text" size="small" class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
                {{ sidebarCollapsed ? '⯈' : '⯆' }}
              </a-button>
            </a-tooltip>
          </div>
        </div>
        <div class="sidebar-content">
          <template v-if="!sidebarCollapsed">
            <!-- Ontology Tab: 本体树 -->
            <OntologyObjectExplorer
              v-if="sidebarTab === 'ontology'"
              :graph-id="effectiveGraphId"
              :ontology-mode="ontologyMode"
              @open-tab="handleOntologyOpenTab"
              @select-class="handleClassSelected"
              @open-episode="handleEpisodeNodeClick"
              @open-community="handleCommunityNodeClick"
            />
            <!-- Episodes Tab: 剧集类型树 -->
            <EpisodeTypeExplorer
              v-else-if="sidebarTab === 'episodes'"
              :graph-id="effectiveGraphId"
              :definition-id="definitionId"
              @select-type="handleEpisodeTypeSelect"
              @create-type="handleEpisodeTypeCreate"
            />
            <!-- Communities Tab: 社区树 -->
            <CommunityExplorer
              v-else-if="sidebarTab === 'communities'"
              :graph-id="effectiveGraphId"
              @open-community="handleCommunityNodeClick"
            />
          </template>
          <template v-else>
            <div class="sidebar-collapsed-tabs">
              <button
                class="sidebar-collapsed-tab"
                :class="{ active: sidebarTab === 'ontology' }"
                @click="sidebarCollapsed = false; sidebarTab = 'ontology'"
                :title="t('graphIde.sidebarOntology')"
              >
                O
              </button>
              <button
                class="sidebar-collapsed-tab"
                :class="{ active: sidebarTab === 'episodes' }"
                @click="sidebarCollapsed = false; sidebarTab = 'episodes'"
                :title="t('graphIde.sidebarEpisodes')"
              >
                E
              </button>
              <button
                class="sidebar-collapsed-tab"
                :class="{ active: sidebarTab === 'communities' }"
                @click="sidebarCollapsed = false; sidebarTab = 'communities'"
                :title="t('graphIde.sidebarCommunities')"
              >
                C
              </button>
            </div>
          </template>
        </div>
      </aside>

      <!-- Canvas Area -->
      <div class="ide-canvas" :class="{ collapsed: canvasCollapsed }">
        <template v-if="!canvasCollapsed">
          <!-- OntologyWorkbench：本体模式下显示，支持图谱/列表切换 -->
          <OntologyWorkbench
            v-if="sidebarTab === 'ontology' && ontologyMode === 'class'"
            :graph-id="effectiveGraphId"
            @instance-click="handleOntologyInstanceClick"
            @instance-dblclick="handleOntologyInstanceDblclick"
            @edit-instance="handleOntologyEditInstance"
          />
          <!-- Graph Canvas（其余情况，包括 class-instance-view / class-editor 时） -->
          <template v-else>
          <!-- Toolbar -->
          <div class="canvas-toolbar">
            <div class="toolbar-group">
              <a-tooltip :title="t('graphIde.tooltipAddNode')">
                <a-button type="text" size="small" @click="addNode">
                  <template #icon><PlusOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('graphIde.tooltipAddEdge')">
                <a-button type="text" size="small" @click="addEdge">
                  <template #icon><LinkOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-divider type="vertical" />
              <a-tooltip :title="t('graphIde.tooltipSelect')">
                <a-button
                  type="text"
                  size="small"
                  :class="{ active: currentTool === 'select' }"
                  @click="currentTool = 'select'"
                >
                  <template #icon><AimOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('graphIde.tooltipPan')">
                <a-button
                  type="text"
                  size="small"
                  :class="{ active: currentTool === 'pan' }"
                  @click="currentTool = 'pan'"
                >
                  <template #icon><DragOutlined /></template>
                </a-button>
              </a-tooltip>
            </div>

            <div class="toolbar-separator" />

            <div class="layout-selector">
              <a-tooltip
                v-for="layout in layouts"
                :key="layout.key"
                :title="layout.label"
              >
                <a-button
                  size="small"
                  :class="{ active: currentLayout === layout.key }"
                  @click="currentLayout = layout.key"
                >
                  <template #icon><component :is="layout.icon" style="color: #fff" /></template>
                  <span style="color: #fff">{{ layout.label }}</span>
                </a-button>
              </a-tooltip>
            </div>

            <div class="toolbar-separator" />

            <div class="toolbar-group">
              <a-tooltip :title="t('graphIde.tooltipMinimap')">
                <a-button
                  type="text"
                  size="small"
                  :class="{ active: showMinimap }"
                  @click="showMinimap = !showMinimap"
                >
                  <template #icon><BorderOutlined style="color: #fff" /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('graphIde.tooltipAggregation')">
                <a-button
                  type="text"
                  size="small"
                  :class="{ active: aggregationMode }"
                  @click="aggregationMode = !aggregationMode"
                >
                  <template #icon><ApartmentOutlined style="color: #fff" /></template>
                </a-button>
              </a-tooltip>
            </div>

            <div class="toolbar-spacer" />

            <div class="toolbar-group">
              <a-input-search
                v-model:value="searchKeyword"
                :placeholder="t('graphIde.searchNodePlaceholder')"
                size="small"
                style="width: 180px"
                @search="handleSearch"
              />
            </div>

            <a-button type="primary" @click="showCascadeModal = true">
              {{ t('graphIde.cascadeEdit') }}
            </a-button>
          </div>

          <!-- Canvas -->
          <div class="canvas-wrapper">
            <div v-if="loading" class="canvas-loading">
              <a-spin size="large" :tip="t('graphIde.canvasLoading')" />
            </div>
            <GraphCanvas
              ref="graphCanvasRef"
              :graph-id="graphId"
              :nodes="nodes"
              :edges="edges"
              :layout="currentLayout"
              :tool="currentTool"
              :show-minimap="showMinimap"
              :aggregation-mode="aggregationMode"
              :selected-node="selectedNode"
              @node-click="handleNodeClick"
              @node-dblclick="handleNodeDblClick"
              @node-contextmenu="handleNodeContextMenu"
            />
          </div>
          </template>
        </template>
        <div v-if="canvasCollapsed" class="canvas-expand-btn-wrap">
          <a-tooltip :title="t('graphIde.expandCanvas')">
            <a-button type="text" size="large" class="canvas-expand-btn" @click="canvasCollapsed = false">
              ⯈
            </a-button>
          </a-tooltip>
        </div>
      </div>

      <!-- Right Panel -->
      <aside class="ide-panel" :class="{ collapsed: panelCollapsed }">
        <template v-if="!panelCollapsed">
          <!-- V5.0: Episode 类型详情面板 -->
          <template v-if="sidebarTab === 'episodes' && selectedEpisodeType">
          <div class="panel-header">
            <span class="panel-title">{{ selectedEpisodeType.typeName || t('graphIde.panelTypeDetail') }}</span>
            <a-button type="text" size="small" @click="selectedEpisodeType = null">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <EpisodeTypeDetailPanel
            :graph-id="effectiveGraphId"
            :type-id="selectedEpisodeType.id"
            :type-data="selectedEpisodeType"
            :pagination="episodePagination"
            :depth="episodeDepth"
            @edit-type="handleEpisodeTypeEdit"
            @delete-type="handleEpisodeTypeDelete"
            @navigate-to-instance="handleNavigateToInstance"
            @pagination-change="handleEpisodePaginationChange"
            @depth-change="handleEpisodeDepthChange"
          />
        </template>

        <!-- V3.0.0: Episode 详情面板 -->
        <template v-else-if="ontologyMode === 'episodes' && selectedEpisode">
          <div class="panel-header">
            <span class="panel-title">{{ t('graphIde.panelEventDetail') }}</span>
            <a-button type="text" size="small" @click="selectedEpisode = null">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <div class="panel-content">
            <a-descriptions :column="2" bordered size="small">
              <a-descriptions-item :label="t('graphIde.labelName')" :span="2">
                {{ selectedEpisode.name }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelType')">
                <a-tag :color="getEpisodeColor(selectedEpisode.episodeType)">
                  {{ selectedEpisode.episodeType || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelProcessType')">
                <a-tag>{{ selectedEpisode.processType || selectedEpisode.legalProcess || '-' }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelStage')">
                {{ selectedEpisode.stageLabel || '-' }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelStageLevel')">
                <a-tag v-if="selectedEpisode.stageLevel || selectedEpisode.courtLevel" color="purple">{{ selectedEpisode.stageLevel || selectedEpisode.courtLevel }}</a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelReviewStage')">
                <a-tag :color="selectedEpisode.isReviewStage || selectedEpisode.isTrialStage ? 'green' : 'default'">
                  {{ (selectedEpisode.isReviewStage || selectedEpisode.isTrialStage) ? t('graphIde.yes') : t('graphIde.no') }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelStartTime')" :span="2">
                {{ formatEpisodeTime(selectedEpisode.startTime) }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelEndTime')" :span="2">
                {{ formatEpisodeTime(selectedEpisode.endTime) }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelContent')" :span="2">
                <div class="episode-content">{{ selectedEpisode.content }}</div>
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </template>

        <!-- V3.0.0: Community 详情面板 -->
        <template v-else-if="ontologyMode === 'communities' && selectedCommunityDetail">
          <div class="panel-header">
            <span class="panel-title">{{ t('graphIde.panelCommunityDetail') }}</span>
            <a-button type="text" size="small" @click="selectedCommunityDetail = null">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <div class="panel-content">
            <a-descriptions :column="2" bordered size="small">
              <a-descriptions-item :label="t('graphIde.labelName')" :span="2">
                {{ selectedCommunityDetail.name }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelType')">
                <a-tag :color="getCommunityColor(selectedCommunityDetail.communityType)">
                  {{ selectedCommunityDetail.communityType || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelLegalDomain')">
                <a-tag>{{ selectedCommunityDetail.legalDomain || '-' }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelJurisdiction')">
                {{ selectedCommunityDetail.jurisdiction || '-' }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelPracticeType')">
                {{ selectedCommunityDetail.practiceType || '-' }}
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelMemberCount')">
                <strong>{{ selectedCommunityDetail.memberCount || 0 }}</strong>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelSummary')" :span="2">
                <div class="community-content">{{ selectedCommunityDetail.summary || '-' }}</div>
              </a-descriptions-item>
              <a-descriptions-item :label="t('graphIde.labelDescription')" :span="2">
                <div class="community-content">{{ selectedCommunityDetail.description || '-' }}</div>
              </a-descriptions-item>
            </a-descriptions>

            <!-- 子社区列表 -->
            <div v-if="selectedCommunityDetail.subCommunities && selectedCommunityDetail.subCommunities.length > 0" class="detail-section" style="margin-top: 16px;">
              <div class="section-title">{{ t('graphIde.subCommunities') }} ({{ selectedCommunityDetail.subCommunities.length }})</div>
              <div class="property-list">
                <div v-for="sub in selectedCommunityDetail.subCommunities" :key="sub.uuid" class="property-item">
                  <span class="property-value">{{ sub.name || sub.uuid }}</span>
                </div>
              </div>
            </div>

            <!-- 成员节点列表 -->
            <div v-if="selectedCommunityDetail.members && selectedCommunityDetail.members.length > 0" class="detail-section" style="margin-top: 16px;">
              <div class="section-title">{{ t('graphIde.memberNodes') }} ({{ selectedCommunityDetail.members.length }})</div>
              <div class="property-list">
                <div v-for="member in selectedCommunityDetail.members.slice(0, 10)" :key="member.uuid" class="property-item" @click="navigateToNode(member.uuid)" style="cursor:pointer">
                  <span class="property-key">{{ member.type || 'Entity' }}</span>
                  <span class="property-value">{{ member.name || member.uuid }}</span>
                </div>
                <div v-if="selectedCommunityDetail.members.length > 10" class="property-item">
                  <span class="property-value" style="color: #8b949e; text-align: center">{{ t('graphIde.moreMembers', { count: selectedCommunityDetail.members.length - 10 }) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="panel-footer">
            <a-popconfirm :title="t('graphIde.confirmDeleteCommunity')" :ok-text="t('graphIde.ok')" :cancel-text="t('graphIde.cancel')" @confirm="deleteSelectedCommunity">
              <a-button danger block>{{ t('graphIde.deleteCommunity') }}</a-button>
            </a-popconfirm>
          </div>
        </template>

        <!-- 左侧类树选择 → 右侧显示类编辑器，不影响中间 OntologyWorkbench -->
        <template v-if="selectedClassId">
          <div class="panel-header">
            <span class="panel-title">类: {{ ontologyStore.classes.find(c => c.id === selectedClassId)?.localName || `#${selectedClassId}` }}</span>
            <a-button type="text" size="small" @click="selectedClassId = null" title="关闭">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <div class="panel-content" style="overflow-y: auto; flex: 1; padding: 0;">
            <ClassEditor
              :class-id="selectedClassId"
              :graph-id="effectiveGraphId"
              @saved="ontologyStore.loadFullOntology(effectiveGraphId)"
            />
          </div>
        </template>

        <template v-else-if="ontologyStore.activeTab && ontologyStore.activeTab.type === 'class-editor'">
          <div class="panel-header">
            <span class="panel-title">{{ ontologyStore.activeTab.title }}</span>
            <a-button type="text" size="small" @click="ontologyStore.closeTab(ontologyStore.activeTabId!)" title="关闭">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <div class="panel-content" style="overflow-y: auto; flex: 1; padding: 0;">
            <ClassEditor
              :class-id="ontologyStore.activeTab.classId"
              :graph-id="effectiveGraphId"
              @saved="ontologyStore.loadFullOntology(effectiveGraphId)"
            />
          </div>
        </template>

        <template v-else-if="ontologyStore.activeTab && ontologyStore.activeTab.type === 'property-editor'">
          <div class="panel-header">
            <span class="panel-title">{{ ontologyStore.activeTab.title }}</span>
            <a-button type="text" size="small" @click="ontologyStore.closeTab(ontologyStore.activeTabId!)" title="关闭">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <div class="panel-content" style="overflow-y: auto; flex: 1; padding: 0;">
            <PropertyEditor
              :property-id="ontologyStore.activeTab.propertyId"
              :graph-id="effectiveGraphId"
              @saved="ontologyStore.loadFullOntology(effectiveGraphId)"
            />
          </div>
        </template>

        <template v-else-if="ontologyStore.activeTab && ontologyStore.activeTab.type === 'constraint-editor'">
          <div class="panel-header">
            <span class="panel-title">{{ ontologyStore.activeTab.title }}</span>
            <a-button type="text" size="small" @click="ontologyStore.closeTab(ontologyStore.activeTabId!)" title="关闭">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>
          <div class="panel-content" style="overflow-y: auto; flex: 1; padding: 0;">
            <ConstraintEditor
              :constraint-id="ontologyStore.activeTab.constraintId"
              :graph-id="effectiveGraphId"
              @saved="ontologyStore.loadFullOntology(effectiveGraphId)"
            />
          </div>
        </template>

        <template v-else-if="selectedNode || selectedClass">
          <div class="panel-header">
            <span class="panel-title">
              {{ selectedNode ? t('graphIde.panelNodeDetail') : t('graphIde.panelClassDetail') }}
            </span>
            <a-button type="text" size="small" @click="closePanel">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>

          <div class="panel-tabs">
            <div
              v-for="tab in detailTabs"
              :key="tab.key"
              class="panel-tab"
              :class="{ active: currentDetailTab === tab.key }"
              @click="currentDetailTab = tab.key"
            >
              {{ tab.label }}
            </div>
          </div>

          <div class="panel-content">
            <!-- Info Tab -->
            <div v-if="currentDetailTab === 'info'" class="detail-section">
              <div class="section-title">{{ t('graphIde.tabBasicInfo') }}</div>
              <div class="info-list">
                <div class="info-row">
                  <span class="info-label">{{ t('graphIde.labelName') }}</span>
                  <span class="info-value">{{ selectedNode?.name || selectedClass?.localName || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">{{ t('graphIde.labelType') }}</span>
                  <a-tag :color="getNodeColor(selectedNode?.type || selectedClass?.localName || '')">
                    {{ selectedNode?.type || selectedClass?.localName }}
                  </a-tag>
                </div>
                <div v-if="selectedNode?.summary" class="info-row">
                  <span class="info-label">{{ t('graphIde.labelSummary') }}</span>
                  <span class="info-value">{{ selectedNode.summary }}</span>
                </div>
                <div v-if="selectedClass?.classUri" class="info-row">
                  <span class="info-label">URI</span>
                  <span class="info-value uri">{{ selectedClass.classUri }}</span>
                </div>
                <div v-if="selectedClass?.description" class="info-row">
                  <span class="info-label">{{ t('graphIde.labelDescription') }}</span>
                  <span class="info-value">{{ selectedClass.description }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">{{ t('graphIde.labelPropertyCount') }}</span>
                  <span class="info-value">{{ selectedClass?.propertyCount || 0 }}</span>
                </div>
                <div v-if="selectedNode?.createdAt" class="info-row">
                  <span class="info-label">{{ t('graphIde.labelCreatedAt') }}</span>
                  <span class="info-value">{{ selectedNode.createdAt }}</span>
                </div>
              </div>
            </div>

            <!-- Properties Tab -->
            <div v-if="currentDetailTab === 'properties'" class="detail-section">
              <div class="section-title">{{ t('graphIde.sectionProperties') }}</div>
              <div class="property-list">
                <div
                  v-for="(value, key) in displayProperties"
                  :key="key"
                  class="property-item"
                >
                  <span class="property-key">{{ key }}</span>
                  <span class="property-value">{{ formatValue(value) }}</span>
                </div>
                <div v-if="Object.keys(displayProperties).length === 0" class="empty-tip">
                  {{ t('graphIde.emptyNoProperties') }}
                </div>
              </div>
              <a-button
                v-if="selectedNode"
                type="link"
                class="add-property-btn"
                @click="showPropertyForm = true"
              >
                <template #icon><PlusOutlined /></template>
                {{ t('graphIde.addProperty') }}
              </a-button>
            </div>

            <!-- Relations Tab -->
            <div v-if="currentDetailTab === 'relations' && selectedNode" class="detail-section">
              <div class="section-title">{{ t('graphIde.sectionRelations') }}</div>
              <div class="relation-list">
                <div
                  v-for="rel in nodeRelations"
                  :key="rel.id"
                  class="relation-item"
                  @click="navigateToNode(rel.targetId)"
                >
                  <a-tag color="purple">{{ rel.type }}</a-tag>
                  <span class="relation-name">{{ rel.targetName }}</span>
                  <RightOutlined class="relation-arrow" />
                </div>
                <div v-if="nodeRelations.length === 0" class="empty-tip">
                  {{ t('graphIde.emptyNoRelations') }}
                </div>
              </div>
            </div>

            <!-- Instances Tab -->
            <div v-if="currentDetailTab === 'instances' && selectedClass" class="detail-section">
              <div class="section-title">{{ t('graphIde.sectionInstances') }}</div>
              
              <div class="instance-toolbar">
                <a-input-search
                  v-model:value="instanceSearchKeyword"
                  :placeholder="t('graphIde.searchInstancePlaceholder')"
                  style="width: 100%"
                  @search="handleInstanceSearch"
                  @change="handleInstanceSearchChange"
                />
              </div>

              <div class="instance-list">
                <div
                  v-for="instance in classInstances"
                  :key="instance.uuid"
                  class="instance-item"
                  :class="{ 
                    'selected': selectedClassInstance?.uuid === instance.uuid,
                    'in-canvas': instance.inCanvas
                  }"
                  @click="handleSelectInstance(instance)"
                >
                  <div class="instance-info">
                    <div class="instance-name">{{ instance.name }}</div>
                    <div class="instance-meta">
                      <a-tag size="small">{{ instance.type }}</a-tag>
                      <span v-if="instance.summary" class="instance-summary">
                        {{ instance.summary.substring(0, 30) }}{{ instance.summary.length > 30 ? '...' : '' }}
                      </span>
                    </div>
                  </div>
                  <div class="instance-actions">
                    <a-tooltip :title="t('graphIde.tooltipLocateInCanvas')">
                      <a-button 
                        type="text" 
                        size="small"
                        @click.stop="locateInstanceInCanvas(instance)"
                      >
                        <template #icon><AimOutlined /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :title="t('graphIde.tooltipAddToCanvas')">
                      <a-button 
                        type="text" 
                        size="small"
                        @click.stop="addInstanceToCanvas(instance)"
                      >
                        <template #icon><PlusOutlined /></template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
                
                <div v-if="instanceLoading" class="instance-loading">
                  <a-spin size="small" />
                  <span>{{ t('graphIde.instanceLoading') }}</span>
                </div>
                
                <div v-if="!instanceLoading && classInstances.length === 0" class="empty-tip">
                  {{ t('graphIde.emptyNoInstances') }}
                </div>
              </div>

              <div v-if="classInstanceTotal > instancePageSize" class="instance-pagination">
                <a-pagination
                  v-model:current="instancePage"
                  :page-size="instancePageSize"
                  :total="classInstanceTotal"
                  size="small"
                  @change="handleInstancePageChange"
                />
              </div>
            </div>
          </div>

          <div class="panel-footer">
            <a-button
              v-if="selectedNode"
              type="primary"
              block
              @click="editSelectedNode"
            >
              {{ t('graphIde.btnEdit') }}
            </a-button>
            <a-popconfirm
              :title="t('graphIde.confirmDelete')"
              :ok-text="t('graphIde.ok')"
              :cancel-text="t('graphIde.cancel')"
              @confirm="deleteSelected"
            >
              <a-button danger block>
                {{ t('graphIde.btnDelete') }}
              </a-button>
            </a-popconfirm>
          </div>
        </template>

        <div v-else-if="!panelCollapsed" class="panel-empty">
          <InboxOutlined class="empty-icon" />
          <div class="empty-title">{{ t('graphIde.emptySelectNodeOrClass') }}</div>
          <div class="empty-desc">{{ t('graphIde.emptyDescClickNode') }}</div>
        </div>
        </template>
        <div v-if="panelCollapsed" class="panel-expand-btn-wrap">
          <a-tooltip :title="t('graphIde.expandPanel')">
            <a-button type="text" size="large" class="panel-expand-btn" @click="panelCollapsed = false">
              ⯇
            </a-button>
          </a-tooltip>
        </div>
      </aside>
    </div>

    <!-- Status Bar -->
    <footer class="ide-status">
      <div class="status-item">
        <span class="status-dot success" />
        <span>{{ t('graphIde.statusNeo4jConnected') }}</span>
      </div>
      <div class="status-item">
        {{ t('graphIde.statusNodes') }}: <strong>{{ formatNumber(graphData?.nodeCount || 0) }}</strong>
      </div>
      <div class="status-item">
        {{ t('graphIde.statusEdges') }}: <strong>{{ formatNumber(graphData?.edgeCount || 0) }}</strong>
      </div>
      <div class="status-item">
        {{ t('graphIde.statusClasses') }}: <strong>{{ schemaClasses.length }}</strong>
      </div>
      <div class="status-item">
        {{ t('graphIde.statusEvents') }}: <strong>{{ graphData?.episodeCount || 0 }}</strong>
      </div>
      <div class="status-spacer" />
      <div class="status-item">
        {{ t('graphIde.statusDisplay') }}: {{ nodes.length }} / {{ formatNumber(graphData?.nodeCount || 0) }}
      </div>
    </footer>

    <!-- Context Menu -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="viewNodeDetails">
        <EyeOutlined /> {{ t('graphIde.contextMenuViewDetail') }}
      </div>
      <div class="context-menu-item" @click="editNodeContext">
        <EditOutlined /> {{ t('graphIde.contextMenuEditProperties') }}
      </div>
      <div class="context-menu-separator" />
      <div class="context-menu-item" @click="expandNeighbors">
        <ExpandOutlined /> {{ t('graphIde.contextMenuExpandNeighbors') }}
      </div>
      <div class="context-menu-item" @click="addRelationContext">
        <LinkOutlined /> {{ t('graphIde.contextMenuAddRelation') }}
      </div>
      <div class="context-menu-separator" />
      <div class="context-menu-item danger" @click="deleteNodeContext">
        <DeleteOutlined /> {{ t('graphIde.contextMenuDeleteNode') }}
      </div>
    </div>

    <!-- Modals -->
    <CascadeEditModal
      v-model:visible="showCascadeModal"
      :graph-id="graphId"
      :classes="schemaClasses"
      @success="handleCascadeSuccess"
    />

    <NodeEditModal
      v-model:visible="showNodeEditModal"
      :graph-id="graphId"
      :node="editingNode"
      :classes="schemaClasses"
      @success="handleNodeEditSuccess"
    />

    <AddEdgeModal
      v-if="addingEdgeSource"
      v-model:visible="showAddEdgeModal"
      :graph-id="graphId"
      :source-node="addingEdgeSource"
      :nodes="nodes"
      @success="handleAddEdgeSuccess"
    />

    <!-- V5.0: Episode Type Edit Modal -->
    <EpisodeTypeEditModal
      v-model:visible="showEpisodeTypeEditModal"
      :graph-id="effectiveGraphId"
      :definition-id="definitionId"
      :type-data="editingEpisodeType"
      :all-types="allEpisodeTypes"
      @success="handleEpisodeTypeEditSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  SyncOutlined,
  SettingOutlined,
  SearchOutlined,
  PlusOutlined,
  LinkOutlined,
  AimOutlined,
  DragOutlined,
  ApartmentOutlined,
  CloseOutlined,
  RightOutlined,
  EyeOutlined,
  EditOutlined,
  ExpandOutlined,
  DeleteOutlined,
  InboxOutlined,
  BorderOutlined,
  NodeIndexOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  ClusterOutlined
} from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import type {
  GraphMetadata,
  GraphIDENode,
  GraphIDEEdge,
  SchemaClass,
  ClassInstance
} from '@/api/graph'
import type { DetailPanelTab, LayoutType, EditTool } from '@/types/graph-ide'
import { communityTypeApi, episodeTypeApi, type OntCommunityTypeVO, type OntEpisodeTypeVO } from '@/api/metadata'
import {
  LEGAL_DOMAIN_COLORS,
  EPISODE_TYPE_COLORS,
  type EpisodeV3,
  type CommunityV3,
} from '@/types/legal-graph-v3'
import GraphCanvas from '@/components/Graph/GraphCanvas.vue'
import CascadeEditModal from '@/components/Graph/CascadeEditModal.vue'
import NodeEditModal from '@/components/Graph/NodeEditModal.vue'
import AddEdgeModal from '@/components/Graph/AddEdgeModal.vue'
import OntologyObjectExplorer from '@/components/Ontology/OntologyObjectExplorer.vue'
import OntologyWorkbench from '@/components/Ontology/OntologyWorkbench.vue'
import ClassEditor from '@/components/Ontology/ClassEditor.vue'
import PropertyEditor from '@/components/Ontology/PropertyEditor.vue'
import ConstraintEditor from '@/components/Ontology/ConstraintEditor.vue'
import EpisodeTypeExplorer from '@/components/Ontology/EpisodeTypeExplorer.vue'
import EpisodeTypeDetailPanel from '@/components/Ontology/EpisodeTypeDetailPanel.vue'
import EpisodeTypeEditModal from '@/components/Ontology/EpisodeTypeEditModal.vue'
import CommunityExplorer from '@/components/Ontology/CommunityExplorer.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// Route params
const graphId = computed(() => route.params.id as string)
const selectedGraphId = ref<string>('')
const graphList = ref<Array<{ graphId: string; name: string }>>([])
const loadingGraphs = ref(false)
const communityTypes = ref<OntCommunityTypeVO[]>([])

// Computed effective graph ID
const effectiveGraphId = computed(() => graphId.value || selectedGraphId.value)

// State
const loading = ref(false)
const syncing = ref(false)
const showSettings = ref(false)

// Sidebar
const sidebarTab = ref<'ontology' | 'episodes' | 'communities'>('ontology')
const ontologyMode = ref<'class' | 'episodes' | 'communities'>('class')

// 三栏折叠状态
const sidebarCollapsed = ref(false)
const canvasCollapsed = ref(false)
const panelCollapsed = ref(false)

// 右侧面板：类编辑器（独立于 ontologyStore.activeTab，不影响中间 OntologyWorkbench）
const selectedClassId = ref<number | null>(null)

// Canvas
const graphCanvasRef = ref()
const currentTool = ref<EditTool>('select')
const currentLayout = ref<LayoutType>('force')
const showMinimap = ref(true)
const aggregationMode = ref(false)
const searchKeyword = ref('')

// Panels
const selectedNode = ref<GraphIDENode | null>(null)
const selectedClass = ref<SchemaClass | null>(null)
const currentDetailTab = ref<DetailPanelTab>('info')

// 本体类视图状态
// Context Menu
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null as GraphIDENode | null
})

// Modals
const showCascadeModal = ref(false)
const showNodeEditModal = ref(false)
const showAddEdgeModal = ref(false)
const showPropertyForm = ref(false)
const editingNode = ref<GraphIDENode | null>(null)
const addingEdgeSource = ref<GraphIDENode | null>(null)

// Data
const graphData = ref<GraphMetadata | null>(null)
const nodes = ref<GraphIDENode[]>([])
const edges = ref<GraphIDEEdge[]>([])
const schemaClasses = ref<SchemaClass[]>([])
const nodeRelations = ref<Array<{ id: string; type: string; targetId: string; targetName: string }>>([])

// Deduplicate nodes and edges by uuid, preserving insertion order
const dedupeNodes = (list: GraphIDENode[]): GraphIDENode[] => {
  return [...new Map(list.map(n => [n.uuid, n])).values()]
}
const dedupeEdges = (list: GraphIDEEdge[]): GraphIDEEdge[] => {
  return [...new Map(list.map(e => [e.uuid, e])).values()]
}

// V5.0: Episode 类型管理
const selectedEpisodeType = ref<OntEpisodeTypeVO | null>(null)
const allEpisodeTypes = ref<OntEpisodeTypeVO[]>([])
const showEpisodeTypeEditModal = ref(false)
const editingEpisodeType = ref<OntEpisodeTypeVO | undefined>(undefined)
const definitionId = ref<number>(0)

// V5.0: Episode 类型分页与深度状态
const episodePagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0,
  hasNextPage: false
})
const episodeDepth = ref<number>(2)

// V3.0.0: 选中的 Episode 详情（剧集视图时使用）
const selectedEpisode = ref<EpisodeV3 | null>(null)

// V3.0.0: 社区详情（社区视图时使用）
const selectedCommunityDetail = ref<any | null>(null)

// Class instances
const classInstances = ref<ClassInstance[]>([])
const classInstanceTotal = ref(0)
const selectedClassInstance = ref<ClassInstance | null>(null)
const instanceLoading = ref(false)
const instancePage = ref(1)
const instancePageSize = ref(20)
const instanceSearchKeyword = ref('')

// Constants
// Constants - 布局类型说明:
// - force (力导向): 基于物理模拟的力导向算法，节点相互排斥，边像弹簧一样连接
// - grid (网格): 节点均匀分布在网格中，适合结构化数据展示
// - dagre (层次): 有向无环图布局，节点按层级从左到右或从上到下排列
// - concentric (同心): 节点按度数排列在同心圆上，中心节点最重要
const layouts = [
  { key: 'force' as LayoutType, label: t('graphIde.layoutForce'), icon: NodeIndexOutlined },
  { key: 'grid' as LayoutType, label: t('graphIde.layoutGrid'), icon: AppstoreOutlined },
  { key: 'dagre' as LayoutType, label: t('graphIde.layoutDagre'), icon: UnorderedListOutlined },
  { key: 'concentric' as LayoutType, label: t('graphIde.layoutConcentric'), icon: ClusterOutlined }
]

const detailTabs = [
  { key: 'info' as DetailPanelTab, label: t('graphIde.tabBasicInfo') },
  { key: 'properties' as DetailPanelTab, label: t('graphIde.tabProperties') },
  { key: 'relations' as DetailPanelTab, label: t('graphIde.tabRelations') },
  { key: 'instances' as DetailPanelTab, label: t('graphIde.tabInstances') }
]

const displayProperties = computed(() => {
  if (selectedNode.value?.properties) return selectedNode.value.properties
  if (selectedClass.value?.properties) {
    const props: Record<string, any> = {}
    selectedClass.value.properties?.forEach(p => {
      props[p.localName] = p.rangeDataType || p.propertyType
    })
    return props
  }
  return {}
})

// Methods
const formatNumber = (num: number): string => {
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const formatValue = (value: any): string => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

// V3.0.0: 根据法律领域获取社区颜色（优先从元数据读取）
const getCommunityColor = (domain?: string): string => {
  if (!domain) return '#999'
  // 优先从 communityTypes 元数据中读取颜色
  const found = communityTypes.value.find(t => t.typeName === domain || t.typeCode === domain)
  if (found?.metadata && typeof found.metadata === 'object' && 'color' in found.metadata) {
    return (found.metadata as { color: string }).color
  }
  // 回退到固定颜色映射
  return LEGAL_DOMAIN_COLORS[domain] || '#999'
}

// V3.0.0: 根据 Episode 类型获取颜色
const getEpisodeColor = (type?: string): string => {
  if (!type) return 'blue'
  return EPISODE_TYPE_COLORS[type] || 'blue'
}

// V3.0.0: 格式化 Episode 时间
const formatEpisodeTime = (timeStr?: string): string => {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return timeStr
  }
}

// V3.0.0: 构建社区树形数据
const buildCommunityTree = (communities: any[]): any[] => {
  if (!communities || !communities.length) return []
  const map = new Map<string, any>()
  communities.forEach((c: any) => {
    map.set(c.uuid, {
      key: c.uuid,
      title: c.name,
      color: getCommunityColor(c.legalDomain),
      legalDomain: c.legalDomain,
      jurisdiction: c.jurisdiction,
      practiceType: c.practiceType,
      isLeaf: true,
      children: [],
    })
  })
  const roots: any[] = []
  communities.forEach((c: any) => {
    const node = map.get(c.uuid)
    if (c.parentCommunityUuid && map.has(c.parentCommunityUuid)) {
      const parent = map.get(c.parentCommunityUuid)
      parent.isLeaf = false
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

const getNodeColor = (type: string): string => {
  const colors: Record<string, string> = {
    Person: '#58a6ff',
    Company: '#3fb950',
    Product: '#d29922',
    Order: '#a371f7',
    Location: '#f85149',
    Event: '#8b5cf6',
    Review: '#06b6d4',
    Category: '#84cc16'
  }
  return colors[type] || '#6e7681'
}

const handleNodeClick = (node: GraphIDENode) => {
  selectedNode.value = node
  selectedClass.value = null
  panelCollapsed.value = false
  currentDetailTab.value = 'info'
  loadNodeRelations(node.uuid)
}

const handleNodeDblClick = (node: GraphIDENode) => {
  editingNode.value = node
  showNodeEditModal.value = true
}

const handleNodeContextMenu = (event: MouseEvent, node: GraphIDENode) => {
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = node
}

const viewNodeDetails = () => {
  if (contextMenu.node) {
    handleNodeClick(contextMenu.node)
  }
  contextMenu.visible = false
}

const editNodeContext = () => {
  if (contextMenu.node) {
    editingNode.value = contextMenu.node
    showNodeEditModal.value = true
  }
  contextMenu.visible = false
}

const expandNeighbors = async () => {
  if (contextMenu.node) {
    try {
      const result = await graphApi.expandNeighbors(effectiveGraphId.value, contextMenu.node.uuid)
      nodes.value = dedupeNodes([...(nodes.value || []), ...(result.nodes || [])])
      edges.value = dedupeEdges([...(edges.value || []), ...(result.edges || [])])
      message.success(t('graphIde.messageExpandedNeighbors', { count: result.nodes.length }))
    } catch (error) {
      message.error(t('graphIde.messageExpandNeighborsFailed'))
    }
  }
  contextMenu.visible = false
}

const addRelationContext = () => {
  if (contextMenu.node) {
    addingEdgeSource.value = contextMenu.node
    showAddEdgeModal.value = true
  }
  contextMenu.visible = false
}

const deleteNodeContext = async () => {
  if (contextMenu.node) {
    try {
      await graphApi.deleteNode(effectiveGraphId.value, contextMenu.node.uuid)
      nodes.value = nodes.value.filter(n => n.uuid !== contextMenu.node!.uuid)
      if (selectedNode.value?.uuid === contextMenu.node.uuid) {
        selectedNode.value = null
        panelCollapsed.value = true
      }
      message.success('删除成功')
    } catch (error) {
      message.error('删除失败')
    }
  }
  contextMenu.visible = false
}

const loadNodeRelations = async (uuid: string) => {
  try {
    const detail = await graphApi.getNodeDetail(effectiveGraphId.value, uuid)
    nodeRelations.value = detail.relations || []
  } catch (error) {
    nodeRelations.value = []
  }
}

const closePanel = () => {
  panelCollapsed.value = true
  selectedNode.value = null
  selectedClass.value = null
}

const editSelectedNode = () => {
  if (selectedNode.value) {
    editingNode.value = selectedNode.value
    showNodeEditModal.value = true
  }
}

const deleteSelected = async () => {
  if (selectedNode.value) {
    try {
      await graphApi.deleteNode(effectiveGraphId.value, selectedNode.value.uuid)
      nodes.value = nodes.value.filter(n => n.uuid !== selectedNode.value!.uuid)
      selectedNode.value = null
      panelCollapsed.value = true
      message.success('删除成功')
    } catch (error) {
      message.error('删除失败')
    }
  }
}

const navigateToNode = async (uuid: string) => {
  const node = nodes.value.find(n => n.uuid === uuid)
  if (node) {
    handleNodeClick(node)
  } else {
    try {
      const detail = await graphApi.getNodeDetail(effectiveGraphId.value, uuid)
      handleNodeClick(detail)
    } catch (error) {
      message.error('加载节点失败')
    }
  }
}

const addNode = () => {
  editingNode.value = null
  showNodeEditModal.value = true
}

const addEdge = () => {
  if (selectedNode.value) {
    addingEdgeSource.value = selectedNode.value
  } else {
    message.info('请先选择一个源节点')
    return
  }
  showAddEdgeModal.value = true
}

const handleSearch = () => {
  // 实现搜索功能
  message.info(`搜索: ${searchKeyword.value}`)
}

const handleSync = async () => {
  syncing.value = true
  try {
    // 重新加载 schemaClasses，确保 ontology 视图使用最新的类列表
    await loadSchemaClasses()
    await loadGraphData()
    message.success('同步完成')
  } catch (error) {
    message.error('同步失败')
  } finally {
    syncing.value = false
  }
}

const handleCascadeSuccess = () => {
  loadGraphData()
}

const handleNodeEditSuccess = (updatedNode: GraphIDENode) => {
  const index = nodes.value.findIndex(n => n.uuid === updatedNode.uuid)
  if (index >= 0) {
    nodes.value[index] = updatedNode
  }
  if (selectedNode.value?.uuid === updatedNode.uuid) {
    selectedNode.value = updatedNode
  }
}

const handleAddEdgeSuccess = (newEdge: GraphIDEEdge) => {
  edges.value.push(newEdge)
}

// Load data
const loadGraphList = async () => {
  loadingGraphs.value = true
  try {
    graphList.value = await graphApi.getList()
  } catch (error) {
    console.error('加载图谱列表失败:', error)
  } finally {
    loadingGraphs.value = false
  }
}

const loadGraphMetadata = async () => {
  if (!effectiveGraphId.value) return
  try {
    graphData.value = await graphApi.getGraphMetadata(effectiveGraphId.value)
  } catch (error) {
    console.error('加载图谱元数据失败:', error)
  }
}

const loadGraphData = async () => {
  if (!effectiveGraphId.value) return
  loading.value = true
  try {
    const data = await graphApi.getVisualization(effectiveGraphId.value, {
      layout: currentLayout.value,
      page: 1,
      pageSize: 500
    })
    nodes.value = dedupeNodes(data.nodes || [])
    edges.value = dedupeEdges(data.edges || [])
  } catch (error) {
    console.error('加载图谱数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadSchemaClasses = async () => {
  if (!effectiveGraphId.value) return
  try {
    schemaClasses.value = await graphApi.getSchemaClasses(effectiveGraphId.value)
  } catch (error) {
    console.error('加载类列表失败:', error)
  }
}

// 加载社区类型元数据
const loadCommunityTypes = async () => {
  if (!effectiveGraphId.value) return
  try {
    const res = await communityTypeApi.list(effectiveGraphId.value, 0)
    communityTypes.value = res || []
  } catch (error) {
    console.error('加载社区类型失败:', error)
  }
}

// Load class instances
const loadClassInstances = async () => {
  if (!effectiveGraphId.value || !selectedClass.value) return
  
  instanceLoading.value = true
  try {
    const classType = selectedClass.value.localName
    const result = await graphApi.getClassInstances(effectiveGraphId.value, classType, {
      page: instancePage.value,
      pageSize: instancePageSize.value,
      keyword: instanceSearchKeyword.value || undefined
    })
    
    // Mark instances that are currently in canvas
    classInstances.value = result.data.map(inst => ({
      ...inst,
      inCanvas: nodes.value.some(n => n.uuid === inst.uuid)
    }))
    classInstanceTotal.value = result.total
  } catch (error) {
    console.error('加载类实例失败:', error)
  } finally {
    instanceLoading.value = false
  }
}

// Handle instance search
const handleInstanceSearch = () => {
  instancePage.value = 1
  loadClassInstances()
}

const handleInstanceSearchChange = () => {
  // Debounce could be added here
  if (!instanceSearchKeyword.value) {
    handleInstanceSearch()
  }
}

// Handle instance page change
const handleInstancePageChange = (page: number) => {
  instancePage.value = page
  loadClassInstances()
}

// Handle instance selection
const handleSelectInstance = (instance: ClassInstance) => {
  // If instance is already selected, deselect it
  if (selectedClassInstance.value?.uuid === instance.uuid) {
    selectedClassInstance.value = null
    return
  }
  
  selectedClassInstance.value = instance
  
  // If the instance is in canvas, also select the corresponding node
  const nodeInCanvas = nodes.value.find(n => n.uuid === instance.uuid)
  if (nodeInCanvas) {
    handleNodeClick(nodeInCanvas)
  }
}

// Locate instance in canvas
const locateInstanceInCanvas = (instance: ClassInstance) => {
  // Find the node in canvas
  const node = nodes.value.find(n => n.uuid === instance.uuid)
  if (node) {
    // Select the node
    handleNodeClick(node)
  } else {
    // Add to canvas first
    addInstanceToCanvas(instance)
  }
}

// Add instance to canvas
const addInstanceToCanvas = (instance: ClassInstance) => {
  // Create a node from the instance
  const newNode: GraphIDENode = {
    uuid: instance.uuid,
    name: instance.name,
    type: instance.type,
    properties: instance.properties,
    summary: instance.summary,
    x: 100 + Math.random() * 400,
    y: 100 + Math.random() * 300
  }
  
  // Add to canvas nodes if not already there
  if (!nodes.value.some(n => n.uuid === instance.uuid)) {
    nodes.value.push(newNode)
    // Update inCanvas flag
    classInstances.value = classInstances.value.map(inst =>
      inst.uuid === instance.uuid ? { ...inst, inCanvas: true } : inst
    )
  }
}

// Handle graph selection change
const handleGraphChange = (value: string) => {
  router.push(`/graph/ide/${value}`)
}

// 本体管理控制台：打开Tab
import { useOntologyStore } from '@/store/modules/ontology'
const ontologyStore = useOntologyStore()

const handleOntologyOpenTab = (payload: { type: string; title: string; classId?: number; propertyId?: number; constraintId?: number; classType?: string; schemaClass?: any }) => {
  ontologyStore.openTab({
    id: payload.classId ? `class-instance-${payload.classId}` :
        payload.propertyId ? `property-editor-${payload.propertyId}` :
        payload.constraintId ? `constraint-editor-${payload.constraintId}` :
        `tab-${payload.type}-${Date.now()}`,
    type: payload.type as any,
    title: payload.title,
    classId: payload.classId,
    propertyId: payload.propertyId,
    constraintId: payload.constraintId,
    classType: payload.classType,
    schemaClass: payload.schemaClass
  })
  // 只有打开 class-editor 才切换到类视图模式（覆盖中间窗体）
  if (payload.type === 'class-editor') {
    ontologyMode.value = 'class'
  }
}

// 左侧类树叶子节点点击 → 在右侧面板打开类编辑器，不影响中间窗体
const handleClassSelected = (classId: number) => {
  selectedClassId.value = classId
  panelCollapsed.value = false  // 自动展开右侧面板
}

// OntologyClassView 事件处理
function handleOntologyInstanceClick(node: any) {
  // 可选：选中节点高亮等
  console.debug('[ide] ontology instance-click', node)
}

function handleOntologyInstanceDblclick(node: any) {
  console.debug('[ide] ontology instance-dblclick', node)
}

function handleOntologyEditInstance(data: any) {
  ontologyStore.openTab({
    id: `instance-editor-${data.uuid || 'new'}`,
    type: 'instance-editor',
    title: `实例: ${data.name || '新建'}`,
    classType: data.type
  })
}

// V5.0: 加载 Episode 类型可视化数据
const loadEpisodeTypeVisualization = async (typeCode: string) => {
  const res = await graphApi.getEpisodesVisualizationByType(
    effectiveGraphId.value,
    typeCode,
    episodePagination.value.page,
    episodePagination.value.pageSize,
    episodeDepth.value
  )

  // 追加模式：新数据与现有数据合并（按 uuid 去重）
  nodes.value = dedupeNodes([...nodes.value, ...(res.nodes || [])])
  edges.value = dedupeEdges([...edges.value, ...(res.edges || [])])

  // 更新分页状态
  episodePagination.value = {
    page: res.pagination?.page || episodePagination.value.page,
    pageSize: res.pagination?.pageSize || episodePagination.value.pageSize,
    total: res.pagination?.total || 0,
    totalPages: res.pagination?.totalPages || 0,
    hasNextPage: res.pagination?.page !== undefined && res.pagination?.totalPages !== undefined
      ? res.pagination.page < res.pagination.totalPages
      : false
  }
}

// V5.0: 选择剧集类型 → 加载类型详情 + 可视化数据
const handleEpisodeTypeSelect = async (payload: { typeId: number; typeCode: string; typeName: string }) => {
  // 重置分页和深度
  episodePagination.value = {
    page: 1,
    pageSize: 20,
    total: 0,
    totalPages: 0,
    hasNextPage: false
  }
  episodeDepth.value = 2

  // 清空画布
  nodes.value = []
  edges.value = []
  ontologyMode.value = 'episodes'
  panelCollapsed.value = false
  loading.value = true

  try {
    await Promise.all([
      loadEpisodeTypeVisualization(payload.typeCode),
      episodeTypeApi.get(effectiveGraphId.value, payload.typeId).then(detail => {
        selectedEpisodeType.value = detail
      })
    ])
  } catch (e) {
    console.error('加载类型数据失败:', e)
    message.error('加载类型数据失败')
  } finally {
    loading.value = false
  }
}

// V5.0: 分页变更（同步画布+表格）
const handleEpisodePaginationChange = async (newPage: number) => {
  episodePagination.value.page = newPage
  const typeCode = selectedEpisodeType.value?.typeCode
  if (!typeCode) return

  loading.value = true
  try {
    await loadEpisodeTypeVisualization(typeCode)
  } catch (e) {
    console.error('翻页加载失败:', e)
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

// V5.0: 跳数变更（清空重载）
const handleEpisodeDepthChange = async (newDepth: number) => {
  episodeDepth.value = newDepth
  episodePagination.value.page = 1
  nodes.value = []
  edges.value = []

  const typeCode = selectedEpisodeType.value?.typeCode
  if (typeCode) {
    loading.value = true
    try {
      await loadEpisodeTypeVisualization(typeCode)
    } catch (e) {
      console.error('深度变更加载失败:', e)
      message.error('加载失败')
    } finally {
      loading.value = false
    }
  }
}

const handleEpisodeTypeCreate = () => {
  editingEpisodeType.value = undefined
  showEpisodeTypeEditModal.value = true
}

const handleEpisodeTypeEdit = (typeId: number) => {
  editingEpisodeType.value = selectedEpisodeType.value || undefined
  showEpisodeTypeEditModal.value = true
}

const handleEpisodeTypeDelete = async (typeId: number) => {
  try {
    await episodeTypeApi.delete(effectiveGraphId.value, typeId)
    message.success('类型已删除')
    selectedEpisodeType.value = null
    panelCollapsed.value = true
    nodes.value = []
    edges.value = []
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

const handleEpisodeTypeEditSuccess = async () => {
  if (selectedEpisodeType.value) {
    try {
      const detail = await episodeTypeApi.get(effectiveGraphId.value, selectedEpisodeType.value.id)
      selectedEpisodeType.value = detail
    } catch (e) { /* ignore */ }
  }
}

const handleNavigateToInstance = async (uuid: string) => {
  // 1. 如果节点不在当前画布中，先从后端加载并添加
  const existingNode = nodes.value.find(n => n.uuid === uuid)
  if (!existingNode) {
    try {
      const detail = await graphApi.getNodeDetail(effectiveGraphId.value, uuid)
      nodes.value.push(detail)
      handleNodeClick(detail)
    } catch (error) {
      message.error('加载节点失败')
      return
    }
  } else {
    handleNodeClick(existingNode)
  }

  // 2. 等待画布更新后，高亮并居中定位
  nextTick(() => {
    const focused = graphCanvasRef.value?.focusNode(uuid)
    if (!focused) {
      message.warning('节点定位失败')
    }
  })
}

async function loadDefinitionId() {
  if (!effectiveGraphId.value) return
  try {
    const ontology = await graphApi.getOntology(effectiveGraphId.value)
    definitionId.value = ontology?.definition?.id || 0
    if (definitionId.value) {
      const types = await episodeTypeApi.getTree(effectiveGraphId.value, definitionId.value)
      allEpisodeTypes.value = types || []
    }
  } catch (e) {
    console.error('加载本体定义失败:', e)
    definitionId.value = 0
  }
}

// 点击剧集节点 → 切换到图谱视图显示剧集数据
const handleEpisodeNodeClick = async (payload?: { stageNode?: any; processNode?: any }) => {
  sidebarTab.value = 'episodes'
  ontologyMode.value = 'episodes'
  selectedEpisode.value = null
  selectedEpisodeType.value = null
  panelCollapsed.value = true
  loading.value = true

  try {
    const data = await graphApi.getEpisodesVisualization(effectiveGraphId.value, 100)
    nodes.value = dedupeNodes(data.nodes || [])
    edges.value = dedupeEdges(data.edges || [])

    if (payload?.stageNode?.episodes?.length) {
      const first = payload.stageNode.episodes[0]
      const detail = await graphApi.getEpisodeDetail(effectiveGraphId.value, first.uuid)
      selectedEpisode.value = detail
      panelCollapsed.value = false
    }
  } catch (error) {
    console.error('加载剧集数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 点击社区节点 → 切换到图谱视图显示社区数据
const handleCommunityNodeClick = async (node?: any) => {
  sidebarTab.value = 'communities'
  ontologyMode.value = 'communities'
  selectedCommunityDetail.value = null
  panelCollapsed.value = true
  loading.value = true

  try {
    const data = await graphApi.getCommunitiesVisualization(effectiveGraphId.value, 100)
    nodes.value = dedupeNodes(data.nodes || [])
    edges.value = dedupeEdges(data.edges || [])

    if (node?.children?.length) {
      const first = node.children[0]
      if (first.uuid) {
        const detail = await graphApi.getCommunityDetail(effectiveGraphId.value, first.uuid)
        selectedCommunityDetail.value = detail
        panelCollapsed.value = false
      }
    }
  } catch (error) {
    console.error('加载社区数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 删除选中的社区
const deleteSelectedCommunity = async () => {
  if (!selectedCommunityDetail.value?.uuid) return
  try {
    await graphApi.deleteCommunity(effectiveGraphId.value, selectedCommunityDetail.value.uuid)
    message.success(t('graphIde.deleteSuccess') || '删除成功')
    selectedCommunityDetail.value = null
    // 刷新社区列表
    await handleCommunityNodeClick()
  } catch (error) {
    console.error('删除社区失败:', error)
  }
}

// 事件流点击（顶级节点）
const loadAllData = async () => {
  await Promise.all([
    loadGraphMetadata(),
    loadGraphData(),
    loadSchemaClasses(),
    loadDefinitionId()
  ])
}

// Hide context menu on click outside
const handleDocumentClick = (e: MouseEvent) => {
  if (contextMenu.visible) {
    contextMenu.visible = false
  }
}

// Lifecycle
onMounted(async () => {
  await loadGraphList()
  if (effectiveGraphId.value) {
    await loadAllData()
  }
  await loadCommunityTypes()
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})

// Watch route params changes
watch(() => route.params.id, async (newId) => {
  if (newId) {
    selectedGraphId.value = newId as string
    await loadAllData()
  }
})

// Watch layout changes
watch(currentLayout, () => {
  loadGraphData()
})

// Watch tab changes - load instances when switching to instances tab
watch(currentDetailTab, (newTab) => {
  if (newTab === 'instances' && selectedClass.value) {
    loadClassInstances()
  }
})
</script>

<style scoped lang="less">
.graph-ide-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0d1117;
  color: #e6edf3;
  overflow: hidden;
}

// Header
.ide-header {
  height: 56px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 8px;

    .logo-text {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;

    .breadcrumb-item {
      color: #8b949e;
      cursor: pointer;

      &:hover {
        color: #58a6ff;
      }
    }

    .breadcrumb-sep {
      color: #6e7681;
    }

    .breadcrumb-current {
      color: #e6edf3;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .action-btn {
      background: transparent;
      border: 1px solid #30363d;
      color: #ffffff !important;

      &:hover {
        border-color: #58a6ff;
        color: #58a6ff !important;
      }
    }

    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: linear-gradient(135deg, #58a6ff, #a371f7);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
    }
  }
}

// Main Content
.ide-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// Sidebar
.ide-sidebar {
  width: 260px;
  background: #161b22;
  border-right: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s;

  &.collapsed {
    width: 48px;
  }

  .sidebar-header {
    padding: 12px;
    border-bottom: 1px solid #30363d;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .sidebar-tabs {
    display: flex;
    gap: 4px;
  }

  .sidebar-collapse-btns {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .collapse-btn {
      color: #8b949e;
      font-size: 14px;
      padding: 4px 6px;

      &:hover {
        color: #e6edf3;
        background: #21262d;
      }
    }
  }

  .sidebar-collapsed-tabs {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 8px 4px;
  }

  .sidebar-collapsed-tab {
    width: 36px;
    height: 36px;
    background: transparent;
    border: 1px solid #30363d;
    border-radius: 6px;
    color: #8b949e;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: #e6edf3;
      background: #21262d;
      border-color: #58a6ff;
    }

    &.active {
      background: #21262d;
      color: #58a6ff;
      border-color: #58a6ff;
    }
  }

  .sidebar-tab {
    flex: 1;
    padding: 8px;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: #8b949e;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      background: #21262d;
      color: #e6edf3;
    }
  }

  .sidebar-content {
    flex: 1;
    overflow-y: auto;
    padding: 0;
  }

  :global(.ide-sidebar .tree-node) {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 8px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    transition: background 0.15s;
  }

  :global(.ide-sidebar .tree-node:hover) {
    background: #21262d;
  }

  :global(.ide-sidebar .tree-node.tree-label-active) {
    color: #58a6ff;
  }

  :global(.ide-sidebar .tree-node .tree-icon) {
    width: 16px;
    font-size: 12px;
    flex-shrink: 0;
  }

  :global(.ide-sidebar .tree-node .tree-icon.active) {
    color: #58a6ff;
  }

  :global(.ide-sidebar .tree-node .tree-label) {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  :global(.ide-sidebar .tree-node .tree-badge) {
    font-size: 11px;
    color: #6e7681;
    background: #21262d;
    padding: 2px 6px;
    border-radius: 10px;
  }

  :global(.ide-sidebar .tree-children) {
    padding-left: 20px;
  }
}

// Explorer Tree — all tree nodes share .tree-node / .tree-children defined inside .ide-sidebar
// Canvas
.ide-canvas {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: min-width 0.2s;

  &.collapsed {
    flex: 0;
    min-width: 48px;
    width: 48px;
  }

  .canvas-expand-btn-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .canvas-expand-btn {
    color: #8b949e;
    font-size: 18px;
    padding: 12px;

    &:hover {
      color: #e6edf3;
      background: #21262d;
    }
  }

  .canvas-toolbar {
    height: 48px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    display: flex;
    align-items: center;
    padding: 0 12px;
    gap: 8px;
    flex-shrink: 0;
  }

  .toolbar-group {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .toolbar-separator {
    width: 1px;
    height: 24px;
    background: #30363d;
    margin: 0 8px;
  }

  .toolbar-spacer {
    flex: 1;
  }

  .layout-selector {
    display: flex;
    gap: 4px;

    .ant-btn {
      background: transparent;
      border: 1px solid #30363d;
      color: #8b949e;
      font-size: 12px;
      padding: 4px 12px;
      height: 28px;

      &:hover {
        border-color: #58a6ff;
        color: #58a6ff;
      }

      &.active {
        background: rgba(88, 166, 255, 0.15);
        border-color: #58a6ff;
        color: #58a6ff;
      }
    }
  }

  .canvas-wrapper {
    flex: 1;
    overflow: hidden;
    position: relative;
  }
}

// Right Panel
.ide-panel {
  width: 480px;
  background: #161b22;
  border-left: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s;

  &.collapsed {
    width: 0;
    overflow: hidden;
  }

  .panel-header {
    padding: 12px 16px;
    border-bottom: 1px solid #30363d;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .panel-tabs {
    display: flex;
    border-bottom: 1px solid #30363d;
  }

  .panel-tab {
    flex: 1;
    padding: 10px;
    font-size: 13px;
    color: #8b949e;
    cursor: pointer;
    text-align: center;
    border-bottom: 2px solid transparent;
    transition: all 0.15s;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      color: #58a6ff;
      border-bottom-color: #58a6ff;
    }
  }

  .panel-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    // V3.0.0: Episode 内容样式
    .episode-content {
      max-height: 200px;
      overflow-y: auto;
      font-size: 13px;
      line-height: 1.6;
      color: #c9d1d9;
      word-break: break-word;
      white-space: pre-wrap;
    }
  }

  .panel-footer {
    padding: 12px;
    border-top: 1px solid #30363d;
    display: flex;
    gap: 8px;
  }
}

.detail-section {
  .section-title {
    font-size: 12px;
    font-weight: 600;
    color: #6e7681;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 12px;
  }
}

.info-list {
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 8px 0;
    border-bottom: 1px solid #21262d;
    gap: 12px;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      font-size: 13px;
      color: #8b949e;
      flex-shrink: 0;
    }

    .info-value {
      font-size: 13px;
      color: #e6edf3;
      text-align: right;
      word-break: break-all;

      &.uri {
        font-size: 11px;
        color: #6e7681;
      }
    }
  }
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .property-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    background: #21262d;
    border-radius: 6px;

    .property-key {
      font-size: 12px;
      color: #58a6ff;
      min-width: 60px;
    }

    .property-value {
      flex: 1;
      font-size: 12px;
      color: #e6edf3;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.relation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .relation-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    background: #21262d;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover {
      background: #30363d;
    }

    .relation-name {
      flex: 1;
      font-size: 12px;
      color: #e6edf3;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .relation-arrow {
      color: #6e7681;
      font-size: 10px;
    }
  }
}

.instance-toolbar {
  margin-bottom: 12px;
}

.instance-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 400px;
  overflow-y: auto;

  .instance-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 10px;
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #1c2128;
      border-color: #58a6ff;
    }

    &.selected {
      background: #1c3a5f;
      border-color: #58a6ff;
    }

    &.in-canvas {
      .instance-name::before {
        content: '';
        display: inline-block;
        width: 6px;
        height: 6px;
        background: #3fb950;
        border-radius: 50%;
        margin-right: 6px;
      }
    }

    .instance-info {
      flex: 1;
      min-width: 0;
    }

    .instance-name {
      font-size: 13px;
      color: #e6edf3;
      font-weight: 500;
      margin-bottom: 4px;
    }

    .instance-meta {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .instance-summary {
      font-size: 11px;
      color: #6e7681;
    }

    .instance-actions {
      display: flex;
      gap: 4px;
      opacity: 0;
      transition: opacity 0.2s;

      .ant-btn {
        color: #8b949e;
        
        &:hover {
          color: #58a6ff;
        }
      }
    }

    &:hover .instance-actions {
      opacity: 1;
    }
  }

  .instance-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;
    color: #6e7681;
  }
}

.instance-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

// Community detail content
.community-content {
  max-height: 200px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
  color: #c9d1d9;
  word-break: break-word;
  white-space: pre-wrap;
}

// Empty tree tip (inside sidebar tree)
.empty-tree-tip {
  padding: 8px 12px;
  font-size: 12px;
  color: #6e7681;
  font-style: italic;
}

.empty-tip {
  text-align: center;
  padding: 24px;
  color: #6e7681;
  font-size: 13px;
}

.add-property-btn {
  width: 100%;
  margin-top: 12px;
  color: #58a6ff;
}

.panel-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: #30363d;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #e6edf3;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #6e7681;
  }
}

.panel-expand-btn-wrap {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 100;
}

.panel-expand-btn {
  color: #8b949e;
  font-size: 18px;
  padding: 12px 8px;
  background: #21262d;
  border: 1px solid #30363d;
  border-right: none;
  border-radius: 6px 0 0 6px;

  &:hover {
    color: #e6edf3;
    background: #30363d;
  }
}

// Status Bar
.ide-status {
  height: 28px;
  background: #161b22;
  border-top: 1px solid #30363d;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 20px;
  font-size: 12px;
  color: #8b949e;
  flex-shrink: 0;

  .status-item {
    display: flex;
    align-items: center;
    gap: 6px;

    strong {
      color: #e6edf3;
    }
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.success {
      background: #3fb950;
    }

    &.warning {
      background: #d29922;
    }

    &.error {
      background: #f85149;
    }
  }

  .status-spacer {
    flex: 1;
  }
}

// Context Menu
.context-menu {
  position: fixed;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 4px;
  min-width: 160px;
  z-index: 1000;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);

  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 13px;
    color: #e6edf3;
    cursor: pointer;
    transition: background 0.1s;

    &:hover {
      background: #21262d;
    }

    &.danger {
      color: #f85149;
    }
  }

  .context-menu-separator {
    height: 1px;
    background: #30363d;
    margin: 4px 0;
  }
}

// Override Ant Design
:deep(.ant-btn-text) {
  color: #8b949e;

  &:hover {
    background: #21262d;
    color: #e6edf3;
  }

  &.active {
    color: #58a6ff;
    background: rgba(88, 166, 255, 0.15);
  }
}

:deep(.ant-input) {
  background: #21262d;
  border-color: #30363d;
  color: #e6edf3;

  &::placeholder {
    color: #6e7681;
  }

  &:hover, &:focus {
    border-color: #58a6ff;
  }
}

:deep(.ant-input-search-button) {
  background: #21262d !important;
  border-color: #30363d !important;

  .anticon {
    color: #8b949e;
  }
}

:deep(.ant-tag) {
  background: #21262d;
  border-color: #30363d;
  color: #e6edf3;
}

:deep(.ant-popconfirm) {
  .ant-btn {
    background: transparent;
    border: 1px solid #30363d;
    color: #8b949e;
  }

  .ant-btn-dangerous {
    background: transparent;
    border-color: #f85149;
    color: #f85149;
  }
}

// 树节点激活状态
.tree-icon.active {
  color: #58a6ff !important;
}

.tree-label-active {
  color: #58a6ff !important;
  font-weight: 500;
}

// Canvas loading overlay
.canvas-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(13, 17, 23, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  border-radius: 8px;
}
</style>
