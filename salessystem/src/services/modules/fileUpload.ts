import { http } from '../http';
import { request } from '../request';
import type { FileExistsResult } from '../../types/admin';

export const fileUploadService = {
  checkExists(fileMd5: string, fileName: string) {
    return request<FileExistsResult>({
      url: '/api/file/check-exists',
      method: 'get',
      params: {
        fileMd5,
        fileName,
      },
      authRole: true,
    });
  },

  async uploadFile(file: File, fileMd5?: string) {
    const formData = new FormData();
    formData.append('file', file);
    if (fileMd5) {
      formData.append('fileMd5', fileMd5);
    }

    const response = await http.request({
      url: '/api/file/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      authRole: true,
    });

    const data = response.data as {
      code: number;
      message: string;
      data: string;
      timestamp: number;
    };

    if (data.code !== 200) {
      throw new Error(data.message || '文件上传失败');
    }

    return data.data;
  },
};
