package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oss.constant.CloudConstant;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.SysOssConfig;
import com.ruoyi.system.domain.bo.SysOssConfigBo;
import com.ruoyi.system.domain.vo.SysOssConfigVo;
import com.ruoyi.system.mapper.SysOssConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysOssConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * 云存储配置Service业务层处理
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class SysOssConfigServiceImpl extends ServicePlusImpl<SysOssConfigMapper, SysOssConfig, SysOssConfigVo> implements ISysOssConfigService {

	private final ISysConfigService iSysConfigService;

    @Override
    public SysOssConfigVo queryById(Integer ossConfigId){
        return getVoById(ossConfigId);
    }

    @Override
    public TableDataInfo<SysOssConfigVo> queryPageList(SysOssConfigBo bo) {
        PagePlus<SysOssConfig, SysOssConfigVo> result = pageVo(PageUtils.buildPagePlus(), buildQueryWrapper(bo));
        return PageUtils.buildDataInfo(result);
    }


    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        LambdaQueryWrapper<SysOssConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getConfigKey()), SysOssConfig::getConfigKey, bo.getConfigKey());
        lqw.like(StringUtils.isNotBlank(bo.getBucketName()), SysOssConfig::getBucketName, bo.getBucketName());
		lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysOssConfig::getStatus, bo.getStatus());
		return lqw;
    }

    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig add = BeanUtil.toBean(bo, SysOssConfig.class);
        validEntityBeforeSave(add);
        return save(add);
    }

    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig update = BeanUtil.toBean(bo, SysOssConfig.class);
        validEntityBeforeSave(update);
        return updateById(update);
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysOssConfig entity){
		if (StringUtils.isNotEmpty(entity.getConfigKey())
			&& UserConstants.NOT_UNIQUE.equals(checkConfigKeyUnique(entity))) {
			throw new CustomException("操作配置'" + entity.getConfigKey() + "'失败, 配置key已存在!");
		}
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid) {
    	if(isValid) {
			if (CollUtil.containsAll(ids, CollUtil.newArrayList(1, 2, 3, 4))) {
				throw new CustomException("系统内置, 不可删除!");
			}
		}
        return removeByIds(ids);
    }

	/**
	 * 判断configKey是否唯一
	 */
	private String checkConfigKeyUnique(SysOssConfig sysOssConfig) {
		long ossConfigId = StringUtils.isNull(sysOssConfig.getOssConfigId()) ? -1L : sysOssConfig.getOssConfigId();
		SysOssConfig info = getOne(new LambdaQueryWrapper<SysOssConfig>()
			.select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
			.eq(SysOssConfig::getConfigKey, sysOssConfig.getConfigKey()));
		if (StringUtils.isNotNull(info) && info.getOssConfigId() != ossConfigId) {
			return UserConstants.NOT_UNIQUE;
		}
		return UserConstants.UNIQUE;
	}

	/**
	 * 启用禁用状态
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public int updateOssConfigStatus(SysOssConfigBo bo) {
    	SysConfig sysConfig = iSysConfigService.getOne(new LambdaQueryWrapper<SysConfig>()
				.eq(SysConfig::getConfigKey, CloudConstant.CLOUD_STORAGE_CONFIG_KEY));
    	if(ObjectUtil.isNotNull(sysConfig)){
    		sysConfig.setConfigValue(bo.getConfigKey());
    		iSysConfigService.updateConfig(sysConfig);
		} else {
    		throw new CustomException("缺少'云存储配置KEY'参数!");
		}
		SysOssConfig sysOssConfig = BeanUtil.toBean(bo, SysOssConfig.class);
		baseMapper.update(null, new LambdaUpdateWrapper<SysOssConfig>()
			.set(SysOssConfig::getStatus, "1"));
		return baseMapper.updateById(sysOssConfig);
	}

}
