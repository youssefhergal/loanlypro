/** Rendu markdown minimal pour les réponses IA (titres, listes, gras, code). */
export function mdToHtml(md: string): string {
  let s = md
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/```[\w]*\n?([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`\n]+)`/g, '<code>$1</code>')
    .replace(/^#{4}\s+(.+)$/gm, '<h4>$1</h4>')
    .replace(/^#{3}\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^#{2}\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#{1}\s+(.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^>\s?(.+)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^---+$/gm, '<hr>');

  s = s.replace(/^(\s*[-*+]\s.+)(\n\s*[-*+]\s.+)*/gm, (block) => {
    const items = block.replace(/^\s*[-*+]\s(.+)$/gm, '<li>$1</li>');
    return `<ul>${items}</ul>`;
  });
  s = s.replace(/^(\s*\d+\.\s.+)(\n\s*\d+\.\s.+)*/gm, (block) => {
    const items = block.replace(/^\s*\d+\.\s(.+)$/gm, '<li>$1</li>');
    return `<ol>${items}</ol>`;
  });

  return s
    .split(/\n{2,}/)
    .map((para) => {
      const t = para.trim();
      if (!t || /^<(h[1-6]|ul|ol|pre|blockquote|hr)/.test(t)) return t;
      return `<p>${t.replace(/\n/g, '<br>')}</p>`;
    })
    .join('\n');
}
