import {useState} from 'react';

interface TextModalProps {
    isOpen: boolean;
    onClose: () => void;
    content: string | null;
    title?: string;
}

export function TextModal({isOpen, onClose, content, title = "Information"}: TextModalProps) {
    const [copied, setCopied] = useState(false);

    if (!isOpen) return null;

    const handleCopy = async () => {
        if (content) {
            await navigator.clipboard.writeText(content);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            <div
                className="absolute inset-0 bg-black/50"
                onClick={onClose}
            />
            <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[80vh] flex flex-col">
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
                    <h2 className="text-xl font-semibold">{title}</h2>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handleCopy}
                            className="px-3 py-1 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
                        >
                            {copied ? '✓ Copied' : 'Copy'}
                        </button>
                        <button
                            onClick={onClose}
                            className="text-gray-500 hover:text-gray-700 text-2xl leading-none"
                        >
                            ×
                        </button>
                    </div>
                </div>
                <div className="flex-1 overflow-auto p-6">
                    {content ? (
                        <pre
                            className="text-sm font-mono bg-gray-50 p-4 rounded border border-gray-200 overflow-auto whitespace-pre-wrap">
              {content}
            </pre>
                    ) : (
                        <div className="flex items-center justify-center h-full">
                            <div className="text-gray-500">No content available</div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
