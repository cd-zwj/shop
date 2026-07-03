package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PosSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * POS 会话数据访问接口，提供 POS 会话表（pos_session）的 CRUD 操作。
 * 管理收银终端的会话状态，包括开班、交班、结算等。
 */
@Mapper
public interface PosSessionMapper extends BaseMapper<PosSession> {
}
