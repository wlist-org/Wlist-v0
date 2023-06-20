pub struct PrintTable {
    headers: Vec<(String, usize)>,
    body: Vec<Vec<(String, usize)>>,
    char: Vec<usize>,
    cache: Option<Vec<String>>,
}

pub struct PrintTableCached {
    cache: Vec<String>,
}

pub fn from_slice(vec: &Vec<&str>) -> Vec<String> {
    let mut s = Vec::new();
    for v in vec {
        s.push(String::from(*v));
    }
    s
}

impl PrintTable {
    pub fn create(headers: Vec<String>) -> PrintTable {
        let mut char = vec![0; headers.len()];
        let headers = PrintTable::fill_columns(&mut char, headers);
        PrintTable { headers, body: Vec::new(), char, cache: None }
    }

    pub fn add_body(mut self, body: Vec<String>) -> Self {
        let len = self.headers.len();
        if len != body.len() {
            panic!("Invalid body length. header: {}, body: {}", len, body.len());
        }
        let body = PrintTable::fill_columns(&mut self.char, body);
        self.body.push(body);
        self.cache = None;
        self
    }

    fn fill_columns(char: &mut [usize], row: Vec<String>) -> Vec<(String, usize)> {
        let mut res = Vec::new();
        for (i, value) in row.into_iter().enumerate() {
            let count = PrintTable::count_non_sbc(&value);
            let width = ((value.chars().count() - count) << 1) + count;
            if width > char[i] {
                char[i] = width;
            }
            res.push((value, width));
        }
        res
    }

    fn count_non_sbc(str: &str) -> usize {
        let mut count = 0;
        for u in str.as_bytes() {
            if *u < 127 {
                count += 1;
            }
        }
        count
    }

    fn build_border(&self) -> String {
        let mut builder = Vec::new();
        builder.push('+');
        for i in &self.char {
            builder.extend(format!("{}+", "-".repeat(i + 2)).chars());
        }
        builder.iter().collect::<String>()
    }

    fn build_row(&self, row: &Vec<(String, usize)>) -> String {
        let mut builder = Vec::new();
        builder.push('|');
        let mut i = 0;
        while i < row.len() {
            builder.extend(format!(" {} ", row[i].0).chars());
            builder.extend(format!("{}|", " ".repeat(self.char[i] - row[i].1)).chars());
            i += 1;
        }
        builder.iter().collect::<String>()
    }
    
    fn build_cache(&mut self) {
        if self.cache.is_some() {
            return;
        }
        let mut cache = Vec::new();
        cache.push(self.build_border());
        cache.push(self.build_row(&self.headers));
        for body in &self.body {
            cache.push(self.build_row(body));
        }
        self.cache = Some(cache);
    }
    
    pub fn finish(mut self) -> PrintTableCached {
        self.build_cache();
        PrintTableCached { cache: self.cache.unwrap() }
    }

    pub fn print(&mut self) {
        self.build_cache();
        let cache = self.cache.as_ref().unwrap();
        println!("{}", cache[0]);
        println!("{}", cache[1]);
        println!("{}", cache[0]);
        for line in &cache[2..] {
            println!("{}", line);
        }
        println!("{}", cache[0]);
    }
}

impl PrintTableCached {
    pub fn print(&self) {
        println!("{}", self.cache[0]);
        println!("{}", self.cache[1]);
        println!("{}", self.cache[0]);
        for line in &self.cache[2..] {
            println!("{}", line);
        }
        println!("{}", self.cache[0]);
    }
}