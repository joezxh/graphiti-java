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

// Mock 数据
let mockEntityTypes: EntityType[] = [
  {
    id: 'et-1',
    name: 'Person',
    description: '人物实体',
    properties: [
      { name: 'name', type: 'string', required: true },
      { name: 'age', type: 'int', required: false },
      { name: 'email', type: 'string', required: false }
    ],
    createdAt: '2024-01-15T08:00:00Z',
    updatedAt: '2024-03-20T10:30:00Z'
  },
  {
    id: 'et-2',
    name: 'Company',
    description: '公司实体',
    properties: [
      { name: 'name', type: 'string', required: true },
      { name: 'industry', type: 'string', required: false },
      { name: 'founded', type: 'date', required: false }
    ],
    createdAt: '2024-01-16T09:00:00Z',
    updatedAt: '2024-03-21T11:00:00Z'
  },
  {
    id: 'et-3',
    name: 'Product',
    description: '产品实体',
    properties: [
      { name: 'name', type: 'string', required: true },
      { name: 'price', type: 'float', required: false },
      { name: 'category', type: 'string', required: false }
    ],
    createdAt: '2024-02-01T10:00:00Z',
    updatedAt: '2024-03-22T12:00:00Z'
  }
]

let mockRelationTypes: RelationType[] = [
  {
    id: 'rt-1',
    name: 'WORKS_AT',
    description: '在某公司工作',
    sourceType: 'Person',
    targetType: 'Company',
    properties: [
      { name: 'since', type: 'date', required: false },
      { name: 'position', type: 'string', required: false }
    ],
    directed: true,
    createdAt: '2024-01-15T08:00:00Z',
    updatedAt: '2024-03-20T10:30:00Z'
  },
  {
    id: 'rt-2',
    name: 'FRIEND_WITH',
    description: '朋友关系',
    sourceType: 'Person',
    targetType: 'Person',
    properties: [
      { name: 'since', type: 'date', required: false }
    ],
    directed: false,
    createdAt: '2024-01-16T09:00:00Z',
    updatedAt: '2024-03-21T11:00:00Z'
  },
  {
    id: 'rt-3',
    name: 'PRODUCED_BY',
    description: '产品由某公司生产',
    sourceType: 'Product',
    targetType: 'Company',
    properties: [],
    directed: true,
    createdAt: '2024-02-01T10:00:00Z',
    updatedAt: '2024-03-22T12:00:00Z'
  }
]

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// 本体配置 API
export const ontologyApi = {
  // 获取实体类型列表
  async listEntityTypes(): Promise<EntityType[]> {
    await delay(300)
    return [...mockEntityTypes]
  },

  // 创建实体类型
  async createEntityType(data: Omit<EntityType, 'id' | 'createdAt' | 'updatedAt'>): Promise<EntityType> {
    await delay(300)
    const newType: EntityType = {
      ...data,
      id: `et-${Date.now()}`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    mockEntityTypes.push(newType)
    return newType
  },

  // 更新实体类型
  async updateEntityType(id: string, data: Partial<Omit<EntityType, 'id'>>): Promise<EntityType> {
    await delay(300)
    const idx = mockEntityTypes.findIndex(t => t.id === id)
    if (idx === -1) throw new Error('EntityType not found')
    mockEntityTypes[idx] = { ...mockEntityTypes[idx], ...data, updatedAt: new Date().toISOString() }
    return mockEntityTypes[idx]
  },

  // 删除实体类型
  async deleteEntityType(id: string): Promise<void> {
    await delay(300)
    mockEntityTypes = mockEntityTypes.filter(t => t.id !== id)
  },

  // 获取关系类型列表
  async listRelationTypes(): Promise<RelationType[]> {
    await delay(300)
    return [...mockRelationTypes]
  },

  // 创建关系类型
  async createRelationType(data: Omit<RelationType, 'id' | 'createdAt' | 'updatedAt'>): Promise<RelationType> {
    await delay(300)
    const newType: RelationType = {
      ...data,
      id: `rt-${Date.now()}`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    mockRelationTypes.push(newType)
    return newType
  },

  // 更新关系类型
  async updateRelationType(id: string, data: Partial<Omit<RelationType, 'id'>>): Promise<RelationType> {
    await delay(300)
    const idx = mockRelationTypes.findIndex(t => t.id === id)
    if (idx === -1) throw new Error('RelationType not found')
    mockRelationTypes[idx] = { ...mockRelationTypes[idx], ...data, updatedAt: new Date().toISOString() }
    return mockRelationTypes[idx]
  },

  // 删除关系类型
  async deleteRelationType(id: string): Promise<void> {
    await delay(300)
    mockRelationTypes = mockRelationTypes.filter(t => t.id !== id)
  }
}

export default ontologyApi
