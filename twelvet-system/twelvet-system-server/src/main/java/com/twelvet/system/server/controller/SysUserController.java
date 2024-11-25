package com.twelvet.system.server.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.secure.BCrypt;
import com.pig4cloud.plugin.excel.annotation.RequestExcel;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import com.twelvet.framework.core.application.controller.TWTController;
import com.twelvet.framework.core.application.domain.AjaxResult;
import com.twelvet.framework.core.application.domain.JsonResult;
import com.twelvet.framework.core.constants.UserConstants;
import com.twelvet.framework.jdbc.web.page.TableDataInfo;
import com.twelvet.framework.jdbc.web.utils.PageUtils;
import com.twelvet.framework.log.annotation.Log;
import com.twelvet.framework.log.enums.BusinessType;
import com.twelvet.framework.security.utils.SecurityUtils;
import com.twelvet.framework.utils.StrUtils;
import com.twelvet.framework.utils.TUtils;
import com.twelvet.system.api.domain.SysRole;
import com.twelvet.system.api.domain.SysUser;
import com.twelvet.system.server.service.ISysPermissionService;
import com.twelvet.system.server.service.ISysPostService;
import com.twelvet.system.server.service.ISysRoleService;
import com.twelvet.system.server.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author twelvet
 * @WebSite twelvet.cn
 * @Description: 用户信息
 */
@Tag(description = "SysUserController", name = "用户信息")
@RestController
@RequestMapping("/system/user")
public class SysUserController extends TWTController {

	@Autowired
	private ISysUserService iSysUserService;

	@Autowired
	private ISysRoleService iSysRoleService;

	@Autowired
	private ISysPostService iSysPostService;

	@Autowired
	private ISysPermissionService iSysPermissionService;

	/**
	 * 获取用户列表
	 * @param user SysUser
	 * @return JsonResult<TableDataInfo>
	 */
	@Operation(summary = "获取用户列表")
	@GetMapping("/pageQuery")
	@SaCheckPermission("system:user:list")
	public JsonResult<TableDataInfo<SysUser>> pageQuery(SysUser user) {
		PageUtils.startPage();
		List<SysUser> list = iSysUserService.selectUserList(user);
		return JsonResult.success(PageUtils.getDataTable(list));
	}

	/**
	 * 用户用户导出
	 * @param user SysUser
	 * @return List<SysUser>
	 */
	@ResponseExcel(name = "用户管理")
	@Operation(summary = "用户用户导出")
	@PostMapping("/export")
	@Log(service = "用户管理", businessType = BusinessType.EXPORT)
	@SaCheckPermission("system:user:export")
	public List<SysUser> export(@RequestBody SysUser user) {
		return iSysUserService.selectUserList(user);
	}

	/**
	 * 用户数据导入
	 * @param userList List<SysUser>
	 * @param cover 是否允许覆盖
	 * @return JsonResult<String>
	 * @throws Exception Exception
	 */
	@Operation(summary = "用户数据导入")
	@PostMapping("/importData")
	@Log(service = "用户管理", businessType = BusinessType.IMPORT)
	@SaCheckPermission("system:user:import")
	public JsonResult<String> importData(@RequestExcel List<SysUser> userList, boolean cover,
			BindingResult bindingResult) throws Exception {
		String operName = SecurityUtils.getUsername();
		iSysUserService.importUser(userList, cover, operName);
		return JsonResult.success();
	}

	/**
	 * 导出模板
	 */
	@ResponseExcel(name = "用户数据导入模板")
	@Operation(summary = "导出模板")
	@PostMapping("/exportTemplate")
	public List<SysUser> exportTemplate() {
		return List.of(new SysUser());
	}

	/**
	 * 获取当前用户信息
	 * @return 用户信息
	 */
	@Operation(summary = "获取当前用户信息")
	@GetMapping("getInfo")
	public AjaxResult getInfo() {
		Long userId = SecurityUtils.getLoginUser().getUserId();
		// 角色集合
		Set<String> roles = iSysPermissionService.getRolePermission(userId);
		// 权限集合
		Set<String> permissions = iSysPermissionService.getMenuPermission(userId);

		AjaxResult ajax = AjaxResult.success();
		ajax.put("user", iSysUserService.selectUserById(userId));
		ajax.put("roles", roles);
		ajax.put("permissions", permissions);
		return ajax;
	}

	/**
	 * 根据用户编号获取详细信息
	 * @param userId Long
	 * @return AjaxResult
	 */
	@Operation(summary = "根据用户编号获取详细信息")
	@SaCheckPermission("system:user:query")
	@GetMapping({ "/", "/{userId}" })
	public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
		iSysUserService.checkUserDataScope(userId);
		Map<String, Object> res = new HashMap<>(6);
		List<SysRole> roles = iSysRoleService.selectRoleAll();
		res.put("roles", SysUser.isAdmin(userId) ? roles
				: roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
		res.put("posts", iSysPostService.selectPostAll());
		if (Objects.nonNull(userId)) {
			res.put("staff", iSysUserService.selectUserById(userId));
			res.put("postIds", iSysPostService.selectPostListByUserId(userId));
			res.put("roleIds", iSysRoleService.selectRoleListByUserId(userId));
		}
		return AjaxResult.success(res);
	}

	/**
	 * 新增用户
	 * @param user SysUser
	 * @return JsonResult<String>
	 */
	@Operation(summary = "新增用户")
	@PostMapping
	@Log(service = "用户管理", businessType = BusinessType.INSERT)
	@SaCheckPermission("system:user:insert")
	public JsonResult<String> insert(@Validated @RequestBody SysUser user) {
		if (UserConstants.NOT_UNIQUE.equals(iSysUserService.checkUserNameUnique(user.getUsername()))) {
			return JsonResult.error("新增用户'" + user.getUsername() + "'失败，登录账号已存在");
		}
		else if (UserConstants.NOT_UNIQUE.equals(iSysUserService.checkPhoneUnique(user))) {
			return JsonResult.error("新增用户'" + user.getUsername() + "'失败，手机号码已存在");
		}
		else if (UserConstants.NOT_UNIQUE.equals(iSysUserService.checkEmailUnique(user))) {
			return JsonResult.error("新增用户'" + user.getUsername() + "'失败，邮箱账号已存在");
		}
		user.setCreateBy(SecurityUtils.getUsername());
		user.setPassword(BCrypt.hashpw(user.getPassword()));
		return json(iSysUserService.insertUser(user));
	}

	/**
	 * 修改用户
	 * @param user SysUser
	 * @return JsonResult<String>
	 */
	@Operation(summary = "修改用户")
	@PutMapping
	@Log(service = "用户管理", businessType = BusinessType.UPDATE)
	@SaCheckPermission("system:user:edit")
	public JsonResult<String> edit(@Validated @RequestBody SysUser user) {
		iSysUserService.checkUserAllowed(user);
		iSysUserService.checkUserDataScope(user.getUserId());
		if (StrUtils.isNotEmpty(user.getPhonenumber())
				&& UserConstants.NOT_UNIQUE.equals(iSysUserService.checkPhoneUnique(user))) {
			return JsonResult.error("修改用户'" + user.getUsername() + "'失败，手机号码已存在");
		}
		else if (StrUtils.isNotEmpty(user.getEmail())
				&& UserConstants.NOT_UNIQUE.equals(iSysUserService.checkEmailUnique(user))) {
			return JsonResult.error("修改用户'" + user.getUsername() + "'失败，邮箱账号已存在");
		}
		user.setUpdateBy(SecurityUtils.getUsername());
		return json(iSysUserService.updateUser(user));
	}

	/**
	 * 删除用户
	 * @param userIds Long[]
	 * @return JsonResult<String>
	 */
	@Operation(summary = "删除用户")
	@SaCheckPermission("system:user:remove")
	@Log(service = "用户管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{userIds}")
	public JsonResult<String> remove(@PathVariable Long[] userIds) {
		return json(iSysUserService.deleteUserByIds(userIds));
	}

	/**
	 * 重置密码
	 * @param user SysUser
	 * @return JsonResult<String>
	 */
	@Operation(summary = "重置密码")
	@PutMapping("/resetPwd")
	@Log(service = "用户管理", businessType = BusinessType.UPDATE)
	@SaCheckPermission("system:user:resetPwd")
	public JsonResult<String> resetPwd(@RequestBody SysUser user) {
		iSysUserService.checkUserAllowed(user);
		iSysUserService.checkUserDataScope(user.getUserId());
		user.setPassword(BCrypt.hashpw(user.getPassword()));
		user.setUpdateBy(SecurityUtils.getUsername());
		return json(iSysUserService.resetPwd(user));
	}

	/**
	 * 用户状态修改
	 * @param user SysUser
	 * @return JsonResult<String>
	 */
	@Operation(summary = "用户状态修改")
	@PutMapping("/changeStatus")
	@Log(service = "用户管理", businessType = BusinessType.UPDATE)
	@SaCheckPermission("system:user:update")
	public JsonResult<String> changeStatus(@RequestBody SysUser user) {
		iSysUserService.checkUserAllowed(user);
		iSysUserService.checkUserDataScope(user.getUserId());
		user.setUpdateBy(SecurityUtils.getUsername());
		return json(iSysUserService.updateUserStatus(user));
	}

}
