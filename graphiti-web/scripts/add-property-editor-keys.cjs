/**
 * 添加propertyEditor翻译key到zh-CN.ts
 */
const fs = require('fs')
const path = require('path')

const filePath = path.join(__dirname, '../src/i18n/locales/zh-CN.ts')
let content = fs.readFileSync(filePath, 'utf8')

const propertyEditorKeys = `  propertyEditor: {
    tabBasic: '基本信息',
    tabDomainRange: '定义域/值域',
    tabConstraints: '约束条件',
    tabInheritance: '继承关系',
    tabStats: '使用统计',
    propertyName: '属性名称（localName）',
    propertyNamePlaceholder: '如 name, age',
    propertyUri: '完整URI（propertyUri）',
    propertyType: '属性类型',
    typeDatatype: '数据类型属性（DatatypeProperty）',
    typeObject: '对象属性（ObjectProperty）',
    typeAnnotation: '注解属性（AnnotationProperty）',
    typeTransitive: '可传递属性（TransitiveProperty）',
    typeSymmetric: '对称属性（SymmetricProperty）',
    typeFunctional: '函数属性（FunctionalProperty）',
    parentProperty: '父属性',
    selectParentProperty: '选择父属性',
    inverseOf: '逆属性（inverseOf）',
    selectInverse: '选择逆属性',
    equivalentTo: '等价属性（equivalentTo）',
    selectEquivalent: '选择等价属性',
    description: '描述',
    example: '示例',
    domain: '定义域（domain）— 哪些类可以使用此属性',
    selectClass: '选择类',
    rangeDataType: '值域数据类型（range — DataType）',
    selectDataType: '选择数据类型',
    dataTypeString: '字符串（string）',
    dataTypeInteger: '整数（integer）',
    dataTypeFloat: '浮点数（float）',
    dataTypeBoolean: '布尔值（boolean）',
    dataTypeDate: '日期（date）',
    dataTypeDatetime: '日期时间（datetime）',
    rangeClass: '值域类（range — Object）',
    selectTargetClass: '选择目标类',
    defaultValue: '默认值',
    defaultValuePlaceholder: '默认属性值',
    minCardinality: '最小基数（minCardinality）',
    maxCardinality: '最大基数（maxCardinality）',
    maxCardinalityPlaceholder: '无限制填0',
    required: '必填',
    isMultiple: '允许多值（isMultiple）',
    pattern: '正则约束（pattern）',
    minValue: '最小值',
    maxValue: '最大值',
    allowedValues: '枚举值（allowedValues）',
    allowedValuesPlaceholder: '输入枚举值后回车确认',
    parentPropertyChain: '父属性链',
    noParentProperty: '无父属性（顶级属性）',
    childProperties: '子属性',
    noChildProperties: '无子属性',
    equivalentProperties: '等价属性',
    noEquivalentProperties: '无等价属性',
    usingClassCount: '使用此属性的类数量',
    constraintCount: '关联约束数量',
    usingClasses: '使用此属性的类',
    noUsageRecord: '暂无使用记录',
    propertySaved: '属性已保存',
    propertyDeleted: '属性已删除',
    confirmDelete: '确定删除此属性？',
    errorPropertyName: '请填写属性名称',
    class: '类',
    property: '属性',
  },
`

const pos = content.indexOf('  classEditor:')
content = content.slice(0, pos) + propertyEditorKeys + content.slice(pos)

fs.writeFileSync(filePath, content, 'utf8')
console.log('✅ propertyEditor keys已添加到zh-CN.ts')

