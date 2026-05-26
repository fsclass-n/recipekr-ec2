import os

with open('src/main/resources/templates/index.html', 'r', encoding='utf-8') as f:
    idx_text = f.read()

start_idx = idx_text.find('<!-- ─── NAVBAR ─── -->')
end_idx = idx_text.find('</nav>') + 6
idx_nav = idx_text[start_idx:end_idx]

# Modify hrefs for absolute paths in base.html
idx_nav = idx_nav.replace('href="#hero"', 'href="/#hero"')
idx_nav = idx_nav.replace('href="#features"', 'href="/#features"')

with open('src/main/resources/templates/layout/base.html', 'r', encoding='utf-8') as f:
    base_text = f.read()

start_base = base_text.find('<!-- ─── NAVBAR ─── -->')
end_base = base_text.find('</nav>') + 6

new_base_text = base_text[:start_base] + idx_nav + base_text[end_base:]

with open('src/main/resources/templates/layout/base.html', 'w', encoding='utf-8') as f:
    f.write(new_base_text)
print("Updated base.html navbar")
