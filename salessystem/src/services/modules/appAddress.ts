import { request } from '../request';
import type { Address, AddressPayload } from '../../types/addressNotification';

export const appAddressService = {
  /** 获取当前用户地址列表 */
  list() {
    return request<Address[]>({
      url: '/v1/app/addresses',
      method: 'get',
      authRole: 'user',
    });
  },

  /** 新增地址 */
  create(payload: AddressPayload) {
    return request<Address>({
      url: '/v1/app/addresses',
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },

  /** 更新地址 */
  update(id: number, payload: AddressPayload) {
    return request<Address>({
      url: `/v1/app/addresses/${id}`,
      method: 'put',
      data: payload,
      authRole: 'user',
    });
  },

  /** 删除地址 */
  remove(id: number) {
    return request<void>({
      url: `/v1/app/addresses/${id}`,
      method: 'delete',
      authRole: 'user',
    });
  },

  /** 设为默认地址 */
  setDefault(id: number) {
    return request<Address>({
      url: `/v1/app/addresses/${id}/default`,
      method: 'put',
      authRole: 'user',
    });
  },
};

export default appAddressService;
