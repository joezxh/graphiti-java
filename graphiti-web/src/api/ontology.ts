import request from './request'

// 实体类型
export interface EntityType {
  id: string
  name: string
  description?: string
  properties: PropertyDef[]
  createdAt?: string
  updatedAt?: string
}

// 关系类型
export interface RelationType {
  id: string
  name: string
  description?: string
  sourceType: string
  targetType: string
  properties: PropertyDef[]
  directed: boolean
  createdAt?: string
  updatedAt?: string
}

// 属性定义
export interface PropertyDef {
  name: string
  type: 'string' | 'int' | 'float' | 'boolean' | 'date' | 'list'
  required?: boolean
  description?: string
}

// 后端本体类 VO 映射
interface OntClassRespVO {
  id: number
  classUri: string
  localName: string
  description?: string
  parentClassId?: number
  createdAt: string
  updatedAt?: string
}

// 后端本体属性 VO 映射
interface OntPropertyRespVO {
  id: number
  propertyUri: string
  localName: string
  propertyType: 'OBJECT' | 'DATATYPE' | 'ANNOTATION'
  domainClassId?: number
  rangeClassId?: number
  rangeDataType?: string
  isRequired?: boolean
  createdAt: string
}

// 后端创建/更新类请求
interface CreateOntClassReq {
  classUri: string
  localName: string
  description?: string
  parentClassId?: number
}

// 后端创建/更新属性请求
interface CreateOntPropertyReq {
  propertyUri: string
  localName: string
  propertyType?: 'OBJECT' | 'DATATYPE' | 'ANNOTATION'
  domainClassId?: number
  rangeClassId?: number
  rangeDataType?: string
  isRequired?: boolean
}

// 将后端本体类映射为前端 EntityType
function mapOntClassToEntityType(cls: OntClassRespVO, _graphId: string): EntityType {
  return {
    id: String(cls.id),
    name: cls.localName,
    description: cls.description,
    properties: [],
    createdAt: cls.createdAt,
    updatedAt: cls.updatedAt
  }
}

// 将后端本体属性映射为前端 PropertyDef
function mapOntPropertyToPropertyDef(prop: OntPropertyRespVO): PropertyDef {
  let type: PropertyDef['type'] = 'string'
  if (prop.propertyType === 'DATATYPE' && prop.rangeDataType) {
    if (prop.rangeDataType.includes('int') || prop.rangeDataType.includes('Integer')) type = 'int'
    else if (prop.rangeDataType.includes('float') || prop.rangeDataType.includes('double') || prop.rangeDataType.includes('Decimal')) type = 'float'
    else if (prop.rangeDataType.includes('boolean') || prop.rangeDataType.includes('Boolean')) type = 'boolean'
    else if (prop.rangeDataType.includes('date') || prop.rangeDataType.includes('Date')) type = 'date'
  } else if (prop.propertyType === 'OBJECT') {
    type = 'string' // 对象类型前端用 string 表示
  }
  return {
    name: prop.localName,
    type,
    required: prop.isRequired,
    description: prop.propertyUri
  }
}

// 本体配置 API
export const ontologyApi = {
  /**
   * 获取实体类型列表（从本体类映射）
   * 后端: GET /ontology/{graphId}/classes
   */
  async listEntityTypes(graphId: string): Promise<EntityType[]> {
    const resp = await request.get<OntClassRespVO[]>(`/ontology/${graphId}/classes`)
    return (resp || []).map(cls => mapOntClassToEntityType(cls, graphId))
  },

  /**
   * 创建实体类型（创建本体类）
   * 后端: POST /ontology/{graphId}/classes
   */
  async createEntityType(
    graphId: string,
    data: Omit<EntityType, 'id' | 'createdAt' | 'updatedAt'>
  ): Promise<EntityType> {
    const req: CreateOntClassReq = {
      classUri: `http://graphiti.io/ontology/${data.name}`,
      localName: data.name,
      description: data.description
    }
    const resp = await request.post<{ id: number }>(`/ontology/${graphId}/classes`, req)
    return {
      id: String(resp.id),
      name: data.name,
      description: data.description,
      properties: data.properties,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  },

  /**
   * 更新实体类型（更新本体类）
   * 后端: PUT /ontology/{graphId}/classes/{classId}
   */
  async updateEntityType(
    graphId: string,
    id: string,
    data: Partial<Omit<EntityType, 'id' | 'createdAt' | 'updatedAt'>>
  ): Promise<EntityType> {
    const req: Partial<CreateOntClassReq> = {}
    if (data.name !== undefined) req.localName = data.name
    if (data.description !== undefined) req.description = data.description
    await request.put(`/ontology/${graphId}/classes/${id}`, req)
    return {
      id,
      name: data.name || '',
      description: data.description,
      properties: data.properties || [],
      updatedAt: new Date().toISOString()
    } as EntityType
  },

  /**
   * 删除实体类型（删除本体类）
   * 后端: DELETE /ontology/{graphId}/classes/{classId}
   */
  async deleteEntityType(graphId: string, id: string): Promise<void> {
    await request.delete(`/ontology/${graphId}/classes/${id}`)
  },

  /**
   * 获取关系类型列表（从本体属性映射）
   * 后端: GET /ontology/{graphId}/properties
   */
  async listRelationTypes(graphId: string): Promise<RelationType[]> {
    const resp = await request.get<OntPropertyRespVO[]>(`/ontology/${graphId}/properties`)
    const props: OntPropertyRespVO[] = resp || []
    // 按 localName 分组，构造关系类型
    const byName = new Map<string, OntPropertyRespVO[]>()
    for (const p of props) {
      if (!byName.has(p.localName)) byName.set(p.localName, [])
      byName.get(p.localName)!.push(p)
    }
    const relationTypes: RelationType[] = []
    for (const [name, propList] of byName) {
      const domain = propList.find(p => p.domainClassId)
      const range = propList.find(p => p.rangeClassId)
      relationTypes.push({
        id: String(propList[0].id),
        name,
        description: propList[0].propertyUri,
        sourceType: domain ? String(domain.domainClassId) : '',
        targetType: range ? String(range.rangeClassId) : '',
        properties: propList.map(mapOntPropertyToPropertyDef),
        directed: true,
        createdAt: propList[0].createdAt
      })
    }
    return relationTypes
  },

  /**
   * 创建关系类型（创建本体属性）
   * 后端: POST /ontology/{graphId}/properties
   */
  async createRelationType(
    graphId: string,
    data: Omit<RelationType, 'id' | 'createdAt' | 'updatedAt'>
  ): Promise<RelationType> {
    const req: CreateOntPropertyReq = {
      propertyUri: `http://graphiti.io/property/${data.name}`,
      localName: data.name,
      propertyType: 'OBJECT',
      domainClassId: data.sourceType ? Number(data.sourceType) : undefined,
      rangeClassId: data.targetType ? Number(data.targetType) : undefined,
      isRequired: false
    }
    const resp = await request.post<{ id: number }>(`/ontology/${graphId}/properties`, req)
    return {
      id: String(resp.id),
      ...data,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  },

  /**
   * 更新关系类型（更新本体属性）
   * 后端: PUT /ontology/{graphId}/properties/{propertyId}
   */
  async updateRelationType(
    graphId: string,
    id: string,
    data: Partial<Omit<RelationType, 'id' | 'createdAt' | 'updatedAt'>>
  ): Promise<RelationType> {
    const req: Partial<CreateOntPropertyReq> = {}
    if (data.name !== undefined) req.localName = data.name
    if (data.sourceType !== undefined) req.domainClassId = Number(data.sourceType) || undefined
    if (data.targetType !== undefined) req.rangeClassId = Number(data.targetType) || undefined
    await request.put(`/ontology/${graphId}/properties/${id}`, req)
    return {
      id,
      ...data,
      updatedAt: new Date().toISOString()
    } as RelationType
  },

  /**
   * 删除关系类型（删除本体属性）
   * 后端: DELETE /ontology/{graphId}/properties/{propertyId}
   */
  async deleteRelationType(graphId: string, id: string): Promise<void> {
    await request.delete(`/ontology/${graphId}/properties/${id}`)
  },

  /**
   * 批量设置本体（实体类型 + 关系类型）
   * 后端: POST /ontology/{graphId}
   */
  async set(graphId: string, data: { entities: string; edges: string }): Promise<void> {
    await request.post(`/ontology/${graphId}`, data)
  }
}

export default ontologyApi
