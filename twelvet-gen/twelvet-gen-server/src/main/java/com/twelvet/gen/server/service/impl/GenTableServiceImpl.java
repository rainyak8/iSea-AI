package com.twelvet.gen.server.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.twelvet.framework.core.exception.TWTException;
import com.twelvet.framework.datasource.support.DataSourceConstants;
import com.twelvet.framework.security.utils.SecurityUtils;
import com.twelvet.framework.utils.DateUtils;
import com.twelvet.framework.utils.JacksonUtils;
import com.twelvet.framework.utils.StrUtils;
import com.twelvet.gen.api.constant.GenConstants;
import com.twelvet.gen.api.domain.GenTable;
import com.twelvet.gen.api.domain.GenTableColumn;
import com.twelvet.gen.api.domain.GenTemplate;
import com.twelvet.gen.server.mapper.GenMapper;
import com.twelvet.gen.server.mapper.GenTableColumnMapper;
import com.twelvet.gen.server.mapper.GenTableMapper;
import com.twelvet.gen.server.mapper.GenTemplateMapper;
import com.twelvet.gen.server.service.IGenTableService;
import com.twelvet.gen.server.utils.GenUtils;
import com.twelvet.gen.server.utils.VelocityUtils;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author twelvet
 * @WebSite twelvet.cn
 * @Description: 业务 服务层实现
 */
@Service
public class GenTableServiceImpl implements IGenTableService {

	private static final Logger log = LoggerFactory.getLogger(GenTableServiceImpl.class);

	@Autowired
	private GenTableColumnMapper genTableColumnMapper;

	@Autowired
	private GenTemplateMapper genTemplateMapper;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private GenTableMapper genTableMapper;

	/**
	 * 查询业务信息
	 * @param id 业务ID
	 * @return 业务信息
	 */
	@Override
	public GenTable selectGenTableById(Long id) {
		GenTable genTable = genTableMapper.selectGenTableById(id);
		setTableFromOptions(genTable);
		return genTable;
	}

	/**
	 * 查询业务列表
	 * @param genTable 业务信息
	 * @return 业务集合
	 */
	@Override
	public List<GenTable> selectGenTableList(GenTable genTable) {
		return genTableMapper.selectGenTableList(genTable);
	}

	/**
	 * 查询据库列表
	 * @param genTable 业务信息
	 * @return 数据库表集合
	 */
	@Override
	public List<GenTable> selectDbTableList(GenTable genTable) {
		GenMapper genMapper = GenUtils.getMapper(genTable.getDsName());
		// 手动切换数据源
		DynamicDataSourceContextHolder.push(genTable.getDsName());
		return genMapper.selectDbTableList(genTable);
	}

	/**
	 * 查询据库列表
	 * @param tableNames 表名称组
	 * @return 数据库表集合
	 */
	@Override
	public List<GenTable> selectDbTableListByNames(String dsName, String[] tableNames) {
		GenMapper genMapper = GenUtils.getMapper(dsName);
		// 手动切换数据源
		DynamicDataSourceContextHolder.push(dsName);
		List<GenTable> genTables = genMapper.selectDbTableListByNames(tableNames);
		genTables.forEach(genTable -> genTable.setDsName(dsName));
		return genTables;
	}

	/**
	 * 查询所有表信息
	 * @return 表信息集合
	 */
	@Override
	public List<GenTable> selectGenTableAll() {
		return genTableMapper.selectGenTableAll();
	}

	/**
	 * 修改业务
	 * @param genTable 业务信息
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateGenTable(GenTable genTable) {
		String options = JacksonUtils.toJson(genTable.getParams());
		genTable.setOptions(options);
		int row = genTableMapper.updateGenTable(genTable);
		if (row > 0) {
			for (GenTableColumn cenTableColumn : genTable.getColumns()) {
				genTableColumnMapper.updateGenTableColumn(cenTableColumn);
			}
		}
	}

	/**
	 * 删除业务对象
	 * @param tableIds 需要删除的数据ID
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteGenTableByIds(Long[] tableIds) {
		genTableMapper.deleteGenTableByIds(tableIds);
		genTableColumnMapper.deleteGenTableColumnByIds(tableIds);
	}

	/**
	 * 导入表结构
	 * @param tableList 导入表列表
	 */
	@Override
	public void importGenTable(List<GenTable> tableList) {
		if (tableList.isEmpty()) {
			return;
		}

		// 手动切换回代码生成器数据源
		DynamicDataSourceContextHolder.push(DataSourceConstants.DS_MASTER);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(@NotNull TransactionStatus transactionStatus) {
				String operName = SecurityUtils.getUsername();
				try {
					for (GenTable table : tableList) {
						String tableName = table.getTableName();
						GenUtils.initTable(table, operName);
						genTableMapper.insertGenTable(table);
					}
				}
				catch (Exception e) {
					throw new TWTException("导入失败：" + e.getMessage());
				}
			}
		});

		List<GenTableColumn> genTableColumnList = new ArrayList<>();
		for (GenTable genTable : tableList) {
			GenMapper genMapper = GenUtils.getMapper(genTable.getDsName());
			// 手动切换回代码生成器数据源
			DynamicDataSourceContextHolder.push(genTable.getDsName());
			String tableName = genTable.getTableName();
			// 保存列信息
			List<GenTableColumn> genTableColumns = genMapper.selectDbTableColumnsByName(tableName);

			// 手动切换回代码生成器数据源
			DynamicDataSourceContextHolder.push(DataSourceConstants.DS_MASTER);
			GenUtils.initColumnField(genTableColumns, genTable);

			genTableColumnList.addAll(genTableColumns);
		}

		// 手动切换回代码生成器数据源(需要tableId)
		DynamicDataSourceContextHolder.push(DataSourceConstants.DS_MASTER);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(@NotNull TransactionStatus transactionStatus) {
				try {
					for (GenTableColumn genTableColumn : genTableColumnList) {
						genTableColumnMapper.insertGenTableColumn(genTableColumn);
					}
				}
				catch (Exception e) {
					throw new TWTException("导入失败：" + e.getMessage());
				}
			}
		});

	}

	/**
	 * 预览代码
	 * @param tableId 表编号
	 * @return 预览数据列表
	 */
	@Override
	public List<Map<String, String>> previewCode(Long tableId) {
		// 数据模型
		Map<String, Object> dataModel = getDataModel(tableId);

		Long tplGroupId = (Long) dataModel.get("tplGroupId");

		// 获取模板列表
		List<GenTemplate> templateList = genTemplateMapper.selectGenTemplateListByGroupId(tplGroupId);

		Map<String, Map<String, Object>> generatorConfig = VelocityUtils.getGeneratorConfig();
		Map<String, Object> project = generatorConfig.get("project");
		String frontendPath = (String) project.get("frontendPath");
		String backendPath = (String) project.get("backendPath");
		// 预览模式下, 使用相对路径展示
		dataModel.put("frontendPath", frontendPath);
		dataModel.put("backendPath", backendPath);

		return templateList.stream().map(template -> {
			String templateCode = template.getTemplateCode();
			String generatorPath = template.getGeneratorPath();

			String content = VelocityUtils.renderStr(templateCode, dataModel);
			String path = VelocityUtils.renderStr(generatorPath, dataModel);

			// 使用 map 简化代码
			return new HashMap<String, String>(4) {
				{
					put("code", content);
					put("codePath", path);
				}
			};
		}).collect(Collectors.toList());
	}

	/**
	 * 生成代码（下载方式）
	 * @param tableId 表名称
	 * @return 数据
	 */
	@Override
	public byte[] downloadCode(Long tableId) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);
		generatorCode(tableId, zip);
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}

	/**
	 * 生成代码（自定义路径）
	 * @param tableId 表ID
	 */
	@Override
	public void generatorCode(Long tableId) {
		// 数据模型
		Map<String, Object> dataModel = getDataModel(tableId);

		Long tplGroupId = (Long) dataModel.get("tplGroupId");

		// 获取模板列表
		List<GenTemplate> templateList = genTemplateMapper.selectGenTemplateListByGroupId(tplGroupId);

		templateList.forEach(template -> {
			String templateCode = template.getTemplateCode();
			String generatorPath = template.getGeneratorPath();
			String content = VelocityUtils.renderStr(templateCode, dataModel);
			String path = VelocityUtils.renderStr(generatorPath, dataModel);
			FileUtil.writeUtf8String(content, path);
		});
	}

	/**
	 * 同步数据库
	 * @param tableId 表ID
	 */
	@Override
	public void synchDb(Long tableId) {
		GenTable genTable = genTableMapper.selectGenTableById(tableId);
		List<GenTableColumn> tableColumns = genTable.getColumns();

		List<String> tableColumnNames = tableColumns.stream().map(GenTableColumn::getColumnName).toList();

		GenMapper genMapper = GenUtils.getMapper(genTable.getDbType());

		// 手动切换数据源
		DynamicDataSourceContextHolder.push(genTable.getDsName());
		List<GenTableColumn> dbTableColumns = genMapper.selectDbTableColumnsByName(genTable.getTableName());
		if (StrUtils.isEmpty(dbTableColumns)) {
			throw new TWTException("同步数据失败，原表结构不存在");
		}
		List<String> dbTableColumnNames = dbTableColumns.stream().map(GenTableColumn::getColumnName).toList();

		// 手动切换数据源
		if (!DataSourceConstants.DS_MASTER.equals(genTable.getDsName())) {
			DynamicDataSourceContextHolder.push(DataSourceConstants.DS_MASTER);
		}

		// 执行事务
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(@NotNull TransactionStatus transactionStatus) {
				// 移除存在的字段
				dbTableColumns.removeIf(genTableColumn -> tableColumnNames.contains(genTableColumn.getColumnName()));
				GenUtils.initColumnField(dbTableColumns, genTable);

				dbTableColumns.forEach(column -> {
					genTableColumnMapper.insertGenTableColumn(column);
				});

				List<GenTableColumn> delColumns = tableColumns.stream()
					.filter(column -> !dbTableColumnNames.contains(column.getColumnName()))
					.collect(Collectors.toList());
				if (StrUtils.isNotEmpty(delColumns)) {
					genTableColumnMapper.deleteGenTableColumns(delColumns);
				}
			}
		});
	}

	/**
	 * 批量生成代码（下载方式）
	 * @param tableIds 表ID数组
	 * @return 数据
	 */
	@Override
	public byte[] downloadCode(List<Long> tableIds) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);
		for (Long tableId : tableIds) {
			generatorCode(tableId, zip);
		}
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}

	/**
	 * 查询表信息并生成代码
	 */
	private void generatorCode(Long tableId, ZipOutputStream zip) {
		// 数据模型
		Map<String, Object> dataModel = getDataModel(tableId);

		Long tplGroupId = (Long) dataModel.get("tplGroupId");

		// 获取模板列表
		List<GenTemplate> templateList = genTemplateMapper.selectGenTemplateListByGroupId(tplGroupId);

		Map<String, Map<String, Object>> generatorConfig = VelocityUtils.getGeneratorConfig();
		Map<String, Object> project = generatorConfig.get("project");
		String frontendPath = (String) project.get("frontendPath");
		String backendPath = (String) project.get("backendPath");
		// 预览模式下, 使用相对路径展示
		dataModel.put("frontendPath", frontendPath);
		dataModel.put("backendPath", backendPath);

		for (GenTemplate template : templateList) {
			String templateCode = template.getTemplateCode();
			String generatorPath = template.getGeneratorPath();

			String content = VelocityUtils.renderStr(templateCode, dataModel);
			String path = VelocityUtils.renderStr(generatorPath, dataModel);
			try {
				// 添加到zip
				zip.putNextEntry(new ZipEntry(path));
				IoUtil.writeUtf8(zip, false, content);
				zip.flush();
				zip.closeEntry();
			}
			catch (IOException e) {
				log.error("渲染模板失败，表名：" + dataModel.get("tableName"), e);
			}
		}
	}

	/**
	 * 修改保存参数校验
	 * @param genTable 业务信息
	 */
	@Override
	public void validateEdit(GenTable genTable) {
		if (GenConstants.TPL_TREE.equals(genTable.getTplGroupId())) {
			String options = JacksonUtils.toJson(genTable.getParams());
			Map<String, String> paramsObj = JacksonUtils.readValue(options, Map.class);
			if (StrUtils.isEmpty(paramsObj.get(GenConstants.TREE_CODE))) {
				throw new TWTException("树编码字段不能为空");
			}
			else if (StrUtils.isEmpty(paramsObj.get(GenConstants.TREE_PARENT_CODE))) {
				throw new TWTException("树父编码字段不能为空");
			}
			else if (StrUtils.isEmpty(paramsObj.get(GenConstants.TREE_NAME))) {
				throw new TWTException("树名称字段不能为空");
			}
			else if (GenConstants.TPL_SUB.equals(genTable.getTplGroupId())) {
				if (StrUtils.isEmpty(genTable.getSubTableName())) {
					throw new TWTException("关联子表的表名不能为空");
				}
				else if (StrUtils.isEmpty(genTable.getSubTableFkName())) {
					throw new TWTException("子表关联的外键名不能为空");
				}
			}
		}
	}

	/**
	 * 设置主键列信息
	 * @param table 业务表信息
	 */
	public void setPkColumn(GenTable table) {
		for (GenTableColumn column : table.getColumns()) {
			if (column.isPk()) {
				table.setPkColumn(column);
				break;
			}
		}
		if (StrUtils.isNull(table.getPkColumn())) {
			table.setPkColumn(table.getColumns().get(0));
		}
		if (GenConstants.TPL_SUB.equals(table.getTplGroupId())) {
			for (GenTableColumn column : table.getSubTable().getColumns()) {
				if (column.isPk()) {
					table.getSubTable().setPkColumn(column);
					break;
				}
			}
			if (StrUtils.isNull(table.getSubTable().getPkColumn())) {
				table.getSubTable().setPkColumn(table.getSubTable().getColumns().get(0));
			}
		}
	}

	/**
	 * 设置主子表信息
	 * @param table 业务表信息
	 */
	public void setSubTable(GenTable table) {
		String subTableName = table.getSubTableName();
		if (StrUtils.isNotEmpty(subTableName)) {
			table.setSubTable(genTableMapper.selectGenTableByName(subTableName));
		}
	}

	/**
	 * 设置代码生成其他选项值
	 * @param genTable 设置后的生成对象
	 */
	public void setTableFromOptions(GenTable genTable) {
		Map<String, String> paramsObj = JacksonUtils.readValue(genTable.getOptions(), Map.class);
		if (Objects.nonNull(paramsObj)) {
			String treeCode = paramsObj.get(GenConstants.TREE_CODE);
			String treeParentCode = paramsObj.get(GenConstants.TREE_PARENT_CODE);
			String treeName = paramsObj.get(GenConstants.TREE_NAME);
			String parentMenuId = paramsObj.get(GenConstants.PARENT_MENU_ID);
			String parentMenuName = paramsObj.get(GenConstants.PARENT_MENU_NAME);

			genTable.setTreeCode(treeCode);
			genTable.setTreeParentCode(treeParentCode);
			genTable.setTreeName(treeName);
			genTable.setParentMenuId(parentMenuId);
			genTable.setParentMenuName(parentMenuName);
		}
	}

	/**
	 * 获取数据模型方法
	 * @param tableId 表格 ID
	 * @return 数据模型 Map 对象
	 */
	private Map<String, Object> getDataModel(Long tableId) {
		// 获取表格信息
		GenTable genTable = genTableMapper.selectGenTableById(tableId);
		// 设置主子表信息
		setSubTable(genTable);
		// 设置主键列信息
		setPkColumn(genTable);
		String moduleName = genTable.getModuleName();
		String businessName = genTable.getBusinessName();
		String packageName = genTable.getPackageName();
		String functionName = genTable.getFunctionName();

		// 创建数据模型对象
		Map<String, Object> dataModel = new HashMap<>();

		// 填充数据模型
		dataModel.put("tplGroupId", genTable.getTplGroupId());
		dataModel.put("tableName", genTable.getTableName());
		dataModel.put("functionName", StrUtils.isNotEmpty(functionName) ? functionName : "【请填写功能名称】");
		dataModel.put("ClassName", genTable.getClassName());
		dataModel.put("className", StringUtils.uncapitalize(genTable.getClassName()));
		dataModel.put("moduleName", moduleName);
		dataModel.put("BusinessName", StringUtils.capitalize(genTable.getBusinessName()));
		dataModel.put("businessName", genTable.getBusinessName());
		dataModel.put("basePackage", VelocityUtils.getPackagePrefix(packageName));
		dataModel.put("packageName", packageName);
		dataModel.put("packagePath", packageName.replace(".", File.separator));
		dataModel.put("author", genTable.getFunctionAuthor());
		dataModel.put("datetime", DateUtils.getDate());
		dataModel.put("pkColumn", genTable.getPkColumn());
		dataModel.put("importList", VelocityUtils.getImportList(genTable));
		dataModel.put("permissionPrefix", VelocityUtils.getPermissionPrefix(moduleName, businessName));
		dataModel.put("columns", genTable.getColumns());
		dataModel.put("table", genTable);
		VelocityUtils.setMenuVelocityContext(dataModel, genTable);
		if (GenConstants.TPL_TREE.equals(genTable.getTplGroupId())) {
			VelocityUtils.setTreeVelocityContext(dataModel, genTable);
		}
		if (GenConstants.TPL_SUB.equals(genTable.getTplGroupId())) {
			VelocityUtils.setSubVelocityContext(dataModel, genTable);
		}
		return dataModel;
	}

}
