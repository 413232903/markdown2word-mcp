"""
图片下载器
支持下载网络图片和处理本地图片，用于 Markdown 图片插入 Word
"""

import os
import tempfile
import hashlib
from typing import Optional, Tuple
from urllib.parse import urlparse
import requests
from PIL import Image
from io import BytesIO


class ImageDownloader:
    """图片下载和处理工具类

    功能：
    - 下载网络图片
    - 处理本地图片
    - 图片格式转换
    - 图片尺寸调整
    - 图片缓存
    """

    # 支持的图片格式
    SUPPORTED_FORMATS = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}

    # 默认图片尺寸限制（单位：像素）
    # 增大尺寸限制以适应高清图片，同时避免图片过大
    # A4 页面在 72 DPI 下约为 595x842 像素，考虑高清显示，设置为 1200x1600
    MAX_WIDTH = 1200
    MAX_HEIGHT = 1600

    # 缓存目录
    _cache_dir = None

    @classmethod
    def get_cache_dir(cls) -> str:
        """获取缓存目录

        Returns:
            缓存目录路径
        """
        if cls._cache_dir is None:
            cls._cache_dir = os.path.join(tempfile.gettempdir(), 'md2doc_image_cache')
            os.makedirs(cls._cache_dir, exist_ok=True)
        return cls._cache_dir

    @staticmethod
    def download_image(url: str, timeout: int = 30, max_retries: int = 3) -> Optional[bytes]:
        """下载网络图片

        Args:
            url: 图片 URL
            timeout: 超时时间（秒），默认30秒
            max_retries: 最大重试次数，默认3次

        Returns:
            图片二进制数据，失败返回 None
        """
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }
        
        last_error = None
        for attempt in range(max_retries):
            try:
                print(f"尝试下载图片 (第 {attempt + 1}/{max_retries} 次): {url}")
                response = requests.get(url, headers=headers, timeout=timeout, stream=True)
                response.raise_for_status()

                # 验证是否为图片
                content_type = response.headers.get('content-type', '').lower()
                if content_type and not content_type.startswith('image/'):
                    print(f"警告: URL 返回的不是图片类型: {content_type}")
                    # 仍然尝试处理，因为有些服务器可能不设置正确的 content-type
                
                # 读取内容
                image_data = response.content
                
                # 验证图片数据大小（至少要有一些数据）
                if len(image_data) < 100:  # 至少100字节
                    print(f"警告: 下载的图片数据过小: {len(image_data)} 字节")
                    if attempt < max_retries - 1:
                        continue
                    return None
                
                print(f"图片下载成功: {len(image_data)} 字节, Content-Type: {content_type}")
                return image_data

            except requests.Timeout as e:
                last_error = f"下载超时: {e}"
                print(f"下载图片超时 (第 {attempt + 1}/{max_retries} 次): {url}")
                if attempt < max_retries - 1:
                    print(f"将在 {attempt + 1} 秒后重试...")
                    import time
                    time.sleep(attempt + 1)  # 递增延迟重试
                continue
                
            except requests.RequestException as e:
                last_error = f"网络请求失败: {e}"
                print(f"下载图片失败 (第 {attempt + 1}/{max_retries} 次): {url}, 错误: {e}")
                if attempt < max_retries - 1:
                    import time
                    time.sleep(attempt + 1)
                continue
                
            except Exception as e:
                last_error = f"处理图片时出错: {e}"
                print(f"处理图片时出错 (第 {attempt + 1}/{max_retries} 次): {url}, 错误: {e}")
                if attempt < max_retries - 1:
                    import time
                    time.sleep(attempt + 1)
                continue
        
        print(f"图片下载最终失败 {url}: {last_error}")
        return None

    @staticmethod
    def read_local_image(file_path: str) -> Optional[bytes]:
        """读取本地图片

        Args:
            file_path: 本地图片路径

        Returns:
            图片二进制数据，失败返回 None
        """
        try:
            if not os.path.exists(file_path):
                print(f"图片文件不存在: {file_path}")
                return None

            # 检查文件扩展名
            ext = os.path.splitext(file_path)[1].lower()
            if ext not in ImageDownloader.SUPPORTED_FORMATS:
                print(f"不支持的图片格式: {ext}")
                return None

            with open(file_path, 'rb') as f:
                return f.read()

        except Exception as e:
            print(f"读取本地图片失败 {file_path}: {e}")
            return None

    @staticmethod
    def get_image(source: str) -> Optional[bytes]:
        """获取图片（自动判断网络或本地）

        Args:
            source: 图片源（URL 或本地路径）

        Returns:
            图片二进制数据，失败返回 None
        """
        # 判断是否为 URL
        parsed = urlparse(source)
        if parsed.scheme in ('http', 'https'):
            # 检查缓存
            cache_key = hashlib.md5(source.encode()).hexdigest()
            cache_file = os.path.join(ImageDownloader.get_cache_dir(), f"{cache_key}.cache")

            if os.path.exists(cache_file):
                try:
                    with open(cache_file, 'rb') as f:
                        return f.read()
                except Exception:
                    pass

            # 下载图片
            image_data = ImageDownloader.download_image(source)

            # 缓存图片
            if image_data and cache_file:
                try:
                    with open(cache_file, 'wb') as f:
                        f.write(image_data)
                except Exception:
                    pass

            return image_data
        else:
            # 本地文件
            return ImageDownloader.read_local_image(source)

    @staticmethod
    def resize_image(image_data: bytes, max_width: int = MAX_WIDTH,
                     max_height: int = MAX_HEIGHT) -> Tuple[bytes, int, int]:
        """调整图片尺寸

        Args:
            image_data: 原始图片数据
            max_width: 最大宽度
            max_height: 最大高度

        Returns:
            (调整后的图片数据, 宽度, 高度)
        """
        try:
            # 验证图片数据
            if not image_data or len(image_data) == 0:
                print("错误: 图片数据为空")
                return None, 0, 0
            
            # 打开图片
            img = Image.open(BytesIO(image_data))
            original_width, original_height = img.size
            
            print(f"原始图片尺寸: {original_width}x{original_height}")

            # 计算缩放比例（保持宽高比）
            # 只有当图片超过限制时才缩放，不放大图片
            width_ratio = max_width / original_width if original_width > max_width else 1
            height_ratio = max_height / original_height if original_height > max_height else 1
            ratio = min(width_ratio, height_ratio, 1)  # 不放大图片

            # 如果需要缩放
            if ratio < 1:
                new_width = int(original_width * ratio)
                new_height = int(original_height * ratio)
                print(f"缩放图片到: {new_width}x{new_height} (比例: {ratio:.2f})")
                img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
            else:
                new_width = original_width
                new_height = original_height
                print(f"保持原始尺寸: {new_width}x{new_height}")

            # 转换为 PNG 格式（Word 兼容性最好）
            output = BytesIO()
            # 处理 RGBA 和 P 模式的图片
            if img.mode in ('RGBA', 'LA', 'P'):
                # 创建白色背景
                if img.mode == 'P':
                    img = img.convert('RGBA')
                background = Image.new('RGB', img.size, (255, 255, 255))
                if img.mode == 'RGBA' or img.mode == 'LA':
                    # 使用 alpha 通道作为 mask
                    if img.mode == 'RGBA':
                        background.paste(img, mask=img.split()[3])  # 使用 alpha 通道
                    else:
                        background.paste(img, mask=img.split()[-1])  # LA 模式使用最后一个通道
                img = background
            elif img.mode != 'RGB':
                img = img.convert('RGB')

            # 保存为 PNG 格式
            img.save(output, format='PNG', optimize=True)
            output_data = output.getvalue()
            print(f"图片处理完成: {new_width}x{new_height}, 数据大小: {len(output_data)} 字节")
            return output_data, new_width, new_height

        except Exception as e:
            print(f"调整图片尺寸失败: {e}")
            import traceback
            traceback.print_exc()
            # 如果处理失败，尝试返回原始数据（如果可能）
            try:
                # 尝试获取原始图片尺寸
                img = Image.open(BytesIO(image_data))
                original_width, original_height = img.size
                return image_data, original_width, original_height
            except:
                # 如果连原始尺寸都获取不到，返回 None
                return None, 0, 0

    @staticmethod
    def process_image(source: str, max_width: int = MAX_WIDTH,
                      max_height: int = MAX_HEIGHT) -> Optional[Tuple[bytes, int, int]]:
        """处理图片（下载 + 调整尺寸）

        Args:
            source: 图片源（URL 或本地路径）
            max_width: 最大宽度
            max_height: 最大高度

        Returns:
            (图片数据, 宽度, 高度) 或 None
        """
        try:
            # 获取图片
            image_data = ImageDownloader.get_image(source)
            if not image_data:
                print(f"无法获取图片: {source}")
                return None

            # 验证图片数据
            if len(image_data) == 0:
                print(f"图片数据为空: {source}")
                return None

            # 调整尺寸
            result = ImageDownloader.resize_image(image_data, max_width, max_height)
            
            # 验证处理结果
            if result is None or result[0] is None:
                print(f"图片处理失败: {source}")
                return None
            
            processed_data, width, height = result
            
            # 确保返回的数据有效
            if not processed_data or len(processed_data) == 0:
                print(f"处理后的图片数据为空: {source}")
                return None
            
            if width <= 0 or height <= 0:
                print(f"图片尺寸无效: {width}x{height}, 源: {source}")
                return None
            
            return processed_data, width, height
            
        except Exception as e:
            print(f"处理图片时发生异常: {source}, 错误: {e}")
            import traceback
            traceback.print_exc()
            return None
