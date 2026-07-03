import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Database,
  Search,
  Upload,
  Trash2,
  FileText,
  File,
  Film,
  Music,
  Image,
  X,
  Loader2,
} from 'lucide-react';
import { cn } from '../lib/utils';
import {
  ragDocumentList,
  ragDocumentDelete,
  ragUploadCheck,
  ragUploadFile,
  ragChunkCheck,
  ragChunkUpload,
  ragChunkMerge,
  type RagDocument,
} from '../services/rag';

const CHUNK_THRESHOLD = 10 * 1024 * 1024; // 10MB
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB per chunk

const SOURCE_TYPES = [
  { value: '', label: '全部类型' },
  { value: 'PDF', label: 'PDF' },
  { value: 'WORD', label: 'Word' },
  { value: 'PPT', label: 'PPT' },
  { value: 'EXCEL', label: 'Excel' },
  { value: 'MARKDOWN', label: 'Markdown' },
  { value: 'TEXT', label: '文本' },
  { value: 'IMAGE', label: '图片' },
  { value: 'VIDEO', label: '视频' },
];

function sourceTypeIcon(type: string) {
  if (type === 'PDF' || type === 'WORD' || type === 'EXCEL' || type === 'MARKDOWN' || type === 'TEXT') return FileText;
  if (type === 'PPT') return File;
  if (type === 'VIDEO') return Film;
  if (type === 'IMAGE') return Image;
  if (type === 'AUDIO') return Music;
  return File;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

async function computeFileHash(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('');
}

export default function AdminDocuments() {
  const [documents, setDocuments] = useState<RagDocument[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [keyword, setKeyword] = useState('');
  const [sourceType, setSourceType] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  // 上传相关
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploadStatus, setUploadStatus] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadDocuments = useCallback(async () => {
    setIsLoading(true);
    try {
      const result = await ragDocumentList({ page, pageSize: 15, sourceType: sourceType || undefined, keyword: keyword || undefined });
      setDocuments(result.items ?? []);
      setTotalPages(result.totalPages ?? 1);
      setTotal(result.total ?? 0);
      setError('');
    } catch {
      setError('文档加载失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  }, [page, sourceType, keyword]);

  useEffect(() => { loadDocuments(); }, [loadDocuments]);

  const handleDelete = async (fileHash: string) => {
    if (!window.confirm('确定删除此文档？删除后将从知识库中移除。')) return;
    try {
      await ragDocumentDelete(fileHash);
      loadDocuments();
    } catch {
      setError('删除失败');
    }
  };

  const handleUpload = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setIsUploading(true);
    setUploadProgress(0);
    setUploadStatus('准备上传…');

    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        setUploadStatus(`计算校验 (${i + 1}/${files.length})…`);
        const fileHash = await computeFileHash(file);

        setUploadStatus(`检查重复 (${i + 1}/${files.length})…`);
        const check = await ragUploadCheck(fileHash);
        if (check.exists) {
          setUploadStatus(`${file.name} 已存在，跳过`);
          continue;
        }

        if (file.size <= CHUNK_THRESHOLD) {
          setUploadStatus(`上传 ${file.name}…`);
          setUploadProgress(10);
          await ragUploadFile(file, fileHash);
          setUploadProgress(100);
        } else {
          // 分片上传
          const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
          setUploadStatus(`检查分片 (${file.name})…`);
          const chunkStatus = await ragChunkCheck(fileHash, file.name, file.size, totalChunks);
          const uploadedSet = new Set(chunkStatus.uploadedChunks ?? []);

          for (let chunkNum = 1; chunkNum <= totalChunks; chunkNum++) {
            if (uploadedSet.has(chunkNum)) {
              setUploadProgress(Math.round((chunkNum / totalChunks) * 90));
              continue;
            }
            setUploadStatus(`上传 ${file.name} 分片 ${chunkNum}/${totalChunks}…`);
            const start = (chunkNum - 1) * CHUNK_SIZE;
            const end = Math.min(start + CHUNK_SIZE, file.size);
            const chunk = file.slice(start, end);
            await ragChunkUpload(fileHash, chunkNum, chunk);
            setUploadProgress(Math.round((chunkNum / totalChunks) * 90));
          }

          setUploadStatus(`合并分片 ${file.name}…`);
          await ragChunkMerge(fileHash, file.name);
          setUploadProgress(100);
        }
      }
      setUploadStatus('上传完成！');
      loadDocuments();
    } catch {
      setUploadStatus('上传失败，请重试');
    } finally {
      setIsUploading(false);
      setTimeout(() => { setUploadProgress(null); setUploadStatus(''); setIsUploadOpen(false); }, 1500);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">知识库管理</h1>
          <p className="text-sm text-slate-500 mt-1">管理 RAG 知识库文档，支持多格式上传和分片处理</p>
        </div>
        <button
          onClick={() => setIsUploadOpen(true)}
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-primary transition-colors shadow-sm"
        >
          <Upload size={16} />
          上传文档
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-100 rounded-xl text-sm text-red-700">{error}</div>
      )}

      {/* Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-white border border-slate-200 rounded-2xl p-4">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">文档总数</div>
          <div className="text-2xl font-black text-slate-900">{total}</div>
        </div>
        <div className="bg-white border border-slate-200 rounded-2xl p-4">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">当前筛选</div>
          <div className="text-2xl font-black text-slate-900">{sourceType || '全部'}</div>
        </div>
        <div className="bg-white border border-slate-200 rounded-2xl p-4">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">当前页</div>
          <div className="text-2xl font-black text-slate-900">{page} / {totalPages}</div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 mb-5">
        <div className="relative flex-1 w-full sm:w-auto">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
            placeholder="搜索文档名称…"
            className="w-full pl-9 pr-4 py-2.5 text-sm font-medium bg-white border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
          />
        </div>
        <div className="flex gap-2 flex-wrap">
          {SOURCE_TYPES.map(st => (
            <button
              key={st.value}
              onClick={() => { setSourceType(st.value); setPage(1); }}
              className={cn(
                'px-3 py-1.5 text-xs font-bold rounded-lg transition-colors',
                sourceType === st.value
                  ? 'bg-slate-900 text-white'
                  : 'bg-white text-slate-500 border border-slate-200 hover:border-slate-300',
              )}
            >
              {st.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="px-5 py-3 text-xs font-black text-slate-400 uppercase tracking-wider">文档</th>
                <th className="px-5 py-3 text-xs font-black text-slate-400 uppercase tracking-wider">类型</th>
                <th className="px-5 py-3 text-xs font-black text-slate-400 uppercase tracking-wider">大小</th>
                <th className="px-5 py-3 text-xs font-black text-slate-400 uppercase tracking-wider">上传时间</th>
                <th className="px-5 py-3 text-xs font-black text-slate-400 uppercase tracking-wider text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              {(isLoading ? Array.from({ length: 5 }) : documents).map((raw, idx) => {
                const doc = raw as RagDocument | undefined;
                const isData = doc != null && 'sourceType' in doc;
                const Icon = isData ? sourceTypeIcon(doc.sourceType) : FileText;
                return (
                  <tr key={isData ? doc.fileHash : idx} className="border-b border-slate-50 hover:bg-slate-50/50 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary">
                          <Icon size={16} />
                        </div>
                        <span className="text-sm font-bold text-slate-700 truncate max-w-[300px]">
                          {isData ? doc.filename : '加载中…'}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="inline-block px-2 py-0.5 text-[10px] font-bold bg-slate-100 text-slate-600 rounded-md">
                        {isData ? doc.sourceType : '--'}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-sm text-slate-500">
                      {isData ? formatFileSize(doc.fileSize) : '--'}
                    </td>
                    <td className="px-5 py-3.5 text-sm text-slate-500">
                      {isData ? new Date(doc.createdAt).toLocaleDateString('zh-CN') : '--'}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      {isData && (
                        <button
                          onClick={() => handleDelete(doc.fileHash)}
                          className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                          title="删除"
                        >
                          <Trash2 size={14} />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {!isLoading && documents.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center text-sm text-slate-400">
                    <Database className="mx-auto mb-2 text-slate-300" size={32} />
                    暂无文档
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-5">
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page <= 1}
            className="px-3 py-1.5 text-xs font-bold text-slate-500 bg-white border border-slate-200 rounded-lg hover:border-slate-300 disabled:opacity-30"
          >
            上一页
          </button>
          <span className="text-xs font-bold text-slate-400">{page} / {totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page >= totalPages}
            className="px-3 py-1.5 text-xs font-bold text-slate-500 bg-white border border-slate-200 rounded-lg hover:border-slate-300 disabled:opacity-30"
          >
            下一页
          </button>
        </div>
      )}

      {/* Upload Modal */}
      <AnimatePresence>
        {isUploadOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm p-4"
            onClick={() => { if (!isUploading) setIsUploadOpen(false); }}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              onClick={e => e.stopPropagation()}
              className="bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6"
            >
              <div className="flex items-center justify-between mb-5">
                <h3 className="text-lg font-black text-slate-900">上传文档</h3>
                <button onClick={() => { if (!isUploading) setIsUploadOpen(false); }} className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg">
                  <X size={18} />
                </button>
              </div>

              <div
                onClick={() => !isUploading && fileInputRef.current?.click()}
                className={cn(
                  'border-2 border-dashed rounded-2xl p-10 text-center cursor-pointer transition-colors',
                  isUploading ? 'border-slate-200 bg-slate-50' : 'border-slate-300 hover:border-primary hover:bg-primary/5',
                )}
              >
                {isUploading ? (
                  <div>
                    <Loader2 className="mx-auto mb-3 text-primary animate-spin" size={32} />
                    <p className="text-sm font-bold text-slate-700">{uploadStatus}</p>
                    {uploadProgress !== null && (
                      <div className="mt-3 w-full bg-slate-200 rounded-full h-2">
                        <div
                          className="bg-primary h-2 rounded-full transition-all duration-300"
                          style={{ width: `${uploadProgress}%` }}
                        />
                      </div>
                    )}
                  </div>
                ) : (
                  <div>
                    <Upload className="mx-auto mb-3 text-slate-400" size={32} />
                    <p className="text-sm font-bold text-slate-700">点击选择文件或拖拽到此处</p>
                    <p className="text-xs text-slate-400 mt-1">支持 PDF、Word、PPT、Excel、Markdown、图片、音视频</p>
                  </div>
                )}
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  className="hidden"
                  onChange={(e) => handleUpload(e.target.files)}
                  accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.md,.txt,.csv,.jpg,.jpeg,.png,.gif,.mp4,.mp3,.wav"
                />
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
